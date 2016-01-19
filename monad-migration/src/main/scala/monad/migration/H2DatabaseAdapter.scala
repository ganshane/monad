// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright (c) 2015 Jun Tsai <jcai@ganshane.com>
 *
 * All rights reserved.
 */
package monad.migration

trait H2AutoIncrementingColumnDefinitionMixin
    extends ColumnDefinition
    with ColumnSupportsAutoIncrement {
  override protected abstract def sql: String = {
    if (isAutoIncrement) super.sql + " AUTO_INCREMENT"
    else super.sql
  }
}

class H2BigintColumnDefinition
  extends DefaultBigintColumnDefinition
  with H2AutoIncrementingColumnDefinitionMixin

class H2IntegerColumnDefinition
  extends DefaultIntegerColumnDefinition
  with H2AutoIncrementingColumnDefinitionMixin

class H2SmallintColumnDefinition
  extends DefaultSmallintColumnDefinition
  with H2AutoIncrementingColumnDefinitionMixin

// H2 does not support size specifiers for the TIMESTAMP data type.
class H2TimestampColumnDefinition
    extends ColumnDefinition
    with ColumnSupportsDefault {
  override val sql = "TIMESTAMP"
}

class H2DatabaseAdapter(override val schemaNameOpt: Option[String])
    extends DatabaseAdapter(schemaNameOpt) {
  override val vendor = H2

  override val quoteCharacter = '`'

  override val unquotedNameConverter = UppercaseUnquotedNameConverter

  override val userFactory = PlainUserFactory

  override val alterTableDropForeignKeyConstraintPhrase = "CONSTRAINT"

  override val addingForeignKeyConstraintCreatesIndex = true

  override val supportsCheckConstraints = false

  override def columnDefinitionFactory(columnType: SqlType,
                                       characterSetOpt: Option[CharacterSet]): ColumnDefinition = {
    columnType match {
      case BigintType =>
        new H2BigintColumnDefinition
      case BlobType =>
        new DefaultBlobColumnDefinition
      case ClobType =>
        new DefaultClobColumnDefinition
      case BooleanType =>
        new DefaultBooleanColumnDefinition
      case CharType =>
        new DefaultCharColumnDefinition
      case DecimalType =>
        new DefaultDecimalColumnDefinition
      case IntegerType =>
        new H2IntegerColumnDefinition
      case SmallintType =>
        new H2SmallintColumnDefinition
      case TimestampType =>
        new H2TimestampColumnDefinition
      case VarbinaryType =>
        new DefaultVarbinaryColumnDefinition
      case VarcharType =>
        new DefaultVarcharColumnDefinition
    }
  }

  override def lockTableSql(schemaNameOpt: Option[String],
                            tableName: String): String = {
    "SELECT * FROM " + quoteTableName(schemaNameOpt, tableName) + " FOR UPDATE"
  }

  override protected def alterColumnSql(schemaNameOpt: Option[String],
                                        columnDefinition: ColumnDefinition): String = {
    new java.lang.StringBuilder(512)
      .append("ALTER TABLE ")
      .append(quoteTableName(schemaNameOpt, columnDefinition.getTableName))
      .append(" MODIFY COLUMN ")
      .append(quoteColumnName(columnDefinition.getColumnName))
      .append(columnDefinition.toSql)
      .toString
  }

  override def removeIndexSql(schemaNameOpt: Option[String],
                              tableName: String,
                              indexName: String): String = {
    new java.lang.StringBuilder(128)
      .append("ALTER TABLE ")
      .append(quoteTableName(schemaNameOpt, tableName))
      .append(" DROP INDEX ")
      .append(quoteIndexName(None, indexName))
      .toString
  }

  /**
   * 对某一列增加注释
   * @param tableName 表名
   * @param columnName  列名
   * @param comment 注释
   * @return 注释的sql
   */
  override def commentColumnSql(tableName: String, columnName: String, comment: String): String = {
    new java.lang.StringBuffer().append("COMMENT ON COLUMN ").append(
      quoteTableName(schemaNameOpt, tableName)).append(".").append(columnName.toUpperCase())
      .append(" IS '").append(comment).append("'").toString();
  }

  /**
   * 对表添加注释
   * @param tableName 表名
   * @param comment 注释
   * @return 注释的sql
   */
  override def commentTableSql(tableName: String, comment: String): String = {
    new java.lang.StringBuffer().append("COMMENT ON TABLE ").append(
      quoteTableName(schemaNameOpt, tableName))
      .append(" IS '").append(comment).append("'").toString();
  }

  override def createTriggerSql(tableName: String,
                                triggerName: String,
                                timingPointOpt: Option[TriggerTimingPoint],
                                triggerFiringOpt: List[TriggerFiring],
                                referencingOpt:Option[Referencing],
                                forEachRowOpt: Option[ForEachRow.type],
                                whenOpt: Option[When])
                               (f: =>String):String= {
    val tableNameQuoted = quoteTableName(tableName)
    val sb = new StringBuilder
    sb.append(s"CREATE TRIGGER ${triggerName} ${timingPointOpt.get} ")
    sb.append(triggerFiringOpt.mkString(" OR "))

    sb.append(s" ON ${tableNameQuoted} ")

    referencingOpt.foreach(x=>sb.append(s" REFERENCING(${x.expr}) "))

    forEachRowOpt.foreach(x=>sb.append(" FOR EACH ROW "))
    if(whenOpt.isDefined){
      logger.warn("h2 doesn't support when in trigger")
    }
    //whenOpt.foreach(x=>sb.append(s" ${x} "))
    //sb.append(" BEGIN ")
    sb.append(f.replaceAll("\n"," "))
    //sb.append(" END;")

    sb.toString()
  }
}
