package monad.migration

import java.sql.{Connection, DatabaseMetaData, ResultSet}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
 * schema dumper
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-28
 */
object SchemaDumper {
  private final val TABLE_TYPE = Array[String] ( "TABLE" )
  private final val TABLE_TYPES =Array[String] ( "TABLE", "VIEW", "SYNONYM" )
  class DumpOption{
    var catalog:String = _
    var schemaPattern:String = _
    var tablePattern:String = _
    var tableTypes = TABLE_TYPES
  }

  // NOTE java.sql.DatabaseMetaData.getTables :
  private final val TABLES_TABLE_CAT = 1
  private final val TABLES_TABLE_SCHEM = 2
  private final val TABLES_TABLE_NAME = 3
  private final val TABLES_TABLE_TYPE = 4

  def tables()(implicit connection:Connection,options:DumpOption):Seq[String]={
    val metaData = connection.getMetaData
    val tablesSet:ResultSet = metaData.getTables(options.catalog, options.schemaPattern, options.tablePattern, options.tableTypes);
    With.autoClosingResultSet(tablesSet){rs=>
      var buffer  = new ListBuffer[String]()
      while(rs.next()){
        buffer += rs.getString(TABLES_TABLE_NAME);
      }

      buffer.sorted.toSeq
    }
  }
  //列定义
  case class Column(name:String,sqlType:SqlType,options:Array[ColumnOption]) extends Ordered[Column]{
    override def compare(that: Column): Int = name.compareTo(that.name)
  }


  protected val COLUMN_NAME = 4
  protected val DATA_TYPE = 5
  protected val TYPE_NAME = 6
  protected val COLUMN_SIZE = 7
  protected val DECIMAL_DIGITS = 9
  protected val COLUMN_DEF = 13
  protected val IS_NULLABLE = 18

  private val timestampReg = "TIMESTAMP([\\(\\d+\\)]*)".r
  protected def typeFromResultSet(resultSet:ResultSet):SqlType ={
    resultSet.getString(TYPE_NAME) match{
      case "BIGINT"=>
        BigintType
      case "BLOB"|"LONGBLOB"|"BYTEA" =>
        BlobType
      case "BOOLEAN" =>
        BooleanType
      case "CHAR" =>
        CharType
      case "DECIMAL" =>
        DecimalType
      case "INTEGER" =>
        IntegerType
      case "SMALLINT" =>
        SmallintType
      case timestampReg(n) =>
        TimestampType
      case "DATE" => //TODO oracle
        TimestampType
      case "VARBINARY"|"VARCHAR FOR BIT DATA"|"RAW" =>
        VarbinaryType
      case "VARCHAR"|"VARCHAR2" =>
        VarcharType
      case "NUMBER" => //for oracle number
        val precision = intFromResultSet(resultSet,COLUMN_SIZE)
        if(precision <=5 )
          SmallintType
        else if(precision <=10)
          IntegerType
        else
          BigintType
      case "LONG" => // oracle
        //throw new UnsupportedColumnTypeException("LONG")
        VarcharType
      case "CLOB" =>
        ClobType

      case other=>
        throw new UnsupportedColumnTypeException(other)
    }
  }
  protected def intFromResultSet(resultSet:ResultSet, column:Int):Int = {
    val precision = resultSet.getInt(column)
    if( precision == 0 && resultSet.wasNull() )  -1 else precision
  }
  def columns(table:String)(implicit databaseAdapter:DatabaseAdapter,connection:Connection,options:DumpOption): Seq[Column] ={
    val metaData = connection.getMetaData
    val primaryKeys = findPrimaryKeys(metaData,table)
    val columns = metaData.getColumns(options.catalog,options.schemaPattern,table,null)
    With.autoClosingResultSet(columns){rs=>
      var buffer  = new ListBuffer[Column]()
      while(rs.next()){
        val columnOptions = new ArrayBuffer[ColumnOption]()
        val name = rs.getString(COLUMN_NAME)
        val defaultValue = rs.getString(COLUMN_DEF)
        if(defaultValue != null)
          columnOptions += Default(defaultValue)

        val sqlType = typeFromResultSet(rs)
        if(sqlType == null)
          throw new RuntimeException("tableName:"+table+" columnName:"+name)

        val precision = intFromResultSet(rs, COLUMN_SIZE);
        val scale = intFromResultSet(rs, DECIMAL_DIGITS);
        if(sqlType == DecimalType){
          if(precision > 0)
          columnOptions += Precision(precision)
          if(scale > 0 )
          columnOptions += Scale(scale)
        }else{
          if(precision > 0)
            columnOptions += Limit(precision)
        }

        //是否为空
        val nullable = rs.getString(IS_NULLABLE).trim() != "NO"
        if(nullable) columnOptions += Nullable else columnOptions += NotNull

        //列注释
        val colCommentSql= databaseAdapter.fetchColumnCommentSql(table,name)
        val commentOpt = fetchComment(colCommentSql)
        commentOpt.foreach(x=> columnOptions+= Comment(x))

        //是否为主键
        if(primaryKeys.contains(name)){
          columnOptions += PrimaryKey
        }

        buffer += Column(name,sqlType,columnOptions.toArray)
      }
      buffer.sorted.toSeq
    }
  }
  def table(table:String)(implicit databaseAdapter:DatabaseAdapter,
                          connection:Connection,
                          sb:StringBuilder,
                          options:DumpOption): Unit ={
    val tableCommentSql= databaseAdapter.fetchTableCommentSql(table)
    val commentOpt = fetchComment(tableCommentSql)

    sb.append(s"""createTable(\"${table}\"""")
    commentOpt.foreach(x=>sb.append(",").append(Comment(x).toTypeString))
    sb.append("){ t=> \n")
    columns(table).foreach { c =>
      sb.append(s"""  t.column(\"${c.name}\",${c.sqlType}""")
      c.options.foreach{o=>
        sb.append(",").append(o.toTypeString)
      }
      sb.append(")\n")
    }
    sb.append("}\n")
  }
  private def fetchComment(sql:String)(implicit connection:Connection): Option[String]={
    val stmt = connection.createStatement();
    With.autoClosingStatement(stmt){s=>
      val resultSet = s.executeQuery(sql)
      With.autoClosingResultSet(resultSet){rs=>
        if(rs.next()) Option(rs.getString(1)) else None
      }
    }
  }
  private def findPrimaryKeys(metaData:DatabaseMetaData , tableName:String)(implicit options:DumpOption): Seq[String]={
    val resultSet = metaData.getPrimaryKeys(options.catalog,options.schemaPattern,tableName)
    With.autoClosingResultSet(resultSet){rs=>
      val buf = new ListBuffer[String]()
      while(rs.next()){
        buf += rs.getString(COLUMN_NAME)
      }
      buf.toSeq
    }
  }
  def main(args:Array[String]): Unit ={
    val driver = "oracle.jdbc.driver.OracleDriver"
    //Class.forName("oracle.jdbc.driver.OracleDriver")
    val url = System.getProperty("dump.jdbc.url")
    val user = System.getProperty("dump.jdbc.user")
    val pass = System.getProperty("dump.jdbc.pass")
    val schema = System.getProperty("dump.jdbc.schema")

    val vendor = Vendor.forDriver(driver)
    implicit val connectionBuilder = new ConnectionBuilder(url, user, pass)
    implicit val databaseAdapter = DatabaseAdapter.forVendor(vendor,Option(schema))
    implicit val options = new DumpOption
    options.schemaPattern=schema

    implicit val sb = new StringBuilder

    connectionBuilder.withConnection(CommitUponReturnOrException){conn=>
      implicit val connection = conn
      tables().foreach(table)
    }
    println(sb)
  }
}
