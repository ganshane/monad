package monad.migration

import java.sql.{DatabaseMetaData, Connection, ResultSet}

import com.apple.jobjc.appkit.NSBitmapImageRep

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
    val metaData = connection.getMetaData;
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
  case class Column(name:String,sqlType:SqlType,options:Array[ColumnOption]){
  }

  protected val COLUMN_NAME = 4
  protected val DATA_TYPE = 5
  protected val TYPE_NAME = 6
  protected val COLUMN_SIZE = 7
  protected val DECIMAL_DIGITS = 9
  protected val COLUMN_DEF = 13
  protected val IS_NULLABLE = 18

  protected def typeFromResultSet(resultSet:ResultSet):SqlType ={
    resultSet.getString(TYPE_NAME) match{
      case "BIGINT"=> //TODO oracle numberic
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
      case "TIMESTAMP" =>
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

      case other=>
        throw new UnsupportedColumnTypeException()
    }
  }
  protected def intFromResultSet(resultSet:ResultSet, column:Int):Int = {
    val precision = resultSet.getInt(column)
    if( precision == 0 && resultSet.wasNull() )  -1 else precision
  }
  def columns(table:String)(implicit connection:Connection,options:DumpOption): Seq[Column] ={
    val metaData = connection.getMetaData
    val primaryKeys = findPrimayKeys(metaData,table)
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

        val nullable = rs.getString(IS_NULLABLE).trim() != "NO"
        if(nullable) columnOptions += Nullable else columnOptions += NotNull

        if(primaryKeys.contains(name)){
          columnOptions += PrimaryKey
        }

        buffer += Column(name,sqlType,columnOptions.toArray)
      }
      buffer.sorted.toSeq
    }
  }
  def table(table:String)(implicit connection:Connection,sb:StringBuilder): Unit ={
    sb.append(s"createTable(\"${table}\"){t=> \n")
    columns(table).foreach { c =>
      sb.append(s"t.column(\"${c.name}\",${c.sqlType}")
      c.options.foreach{o=>
        sb.append(",").append(o.toTypeString())
      }
    }
    sb.append("}")
  }
  private def findPrimayKeys(metaData:DatabaseMetaData , tableName:String)(implicit options:DumpOption): Seq[String]={
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
    implicit val conn:Connection = null
    implicit val options = new DumpOption
    tables()
  }
}
