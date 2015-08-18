// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.face.model.SyncPolicy
import monad.support.services.MonadException

import scala.collection.JavaConversions._

/**
 * 抓取数据接口
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-19
 */
object DataFetcher{
  def apply(rcl:ResourceConfigLike):DataFetcher={
    if (rcl.resourceDefinition.sync.jdbc.driver.indexOf("oracle") > -1) {
      new OracleDataFetcher(rcl)
    } else if (rcl.resourceDefinition.sync.jdbc.driver.indexOf("ibm") > -1) {
      new DB2DataFetcher(rcl)
    } else if (rcl.resourceDefinition.sync.jdbc.driver.indexOf("mysql") > -1) {
      new MySQLDataFetcher(rcl)
    }else{
      new BaseDataFetcher(rcl) {
        override protected def limitSQLString(valueQuery: Option[String]): String = {
          throw new MonadException("database unsupported",MonadSyncExceptionCode.SQL_UNSUPPORTED)
        }
      }
    }
  }
}
trait DataFetcher {
  def buildIncrementSQL():String
  def buildMinValueSQL():String
  def buildMaxValueSQL():String
}
abstract class BaseDataFetcher(rcl:ResourceConfigLike) extends DataFetcher{
  protected val resourceDefinition = rcl.resourceDefinition
  protected val isFullFetch = resourceDefinition.sync.policy == SyncPolicy.Full
  //创建select语句
  protected val selects = resourceDefinition.properties.map(_.name).map { name =>
    if (name.startsWith("_")) "\"" + name + "\"" else name
  }.mkString(",")
  protected val fullQuerySQL="select " + selects + " from (" + resourceDefinition.sync.jdbc.sql + ")"
  override def buildMaxValueSQL(): String = {
    if(isFullFetch)
      limitSQLString(Some("max"))
    else{
      splitSQL() match {
        case (sql,Some(maxSql)) =>
          maxSql
        case (sql,None) =>
          "select max(" + rcl.incrementColumn.name + ") from ( " + sql + " ) x_"
      }
    }
  }
  override def buildMinValueSQL(): String = {
    if(isFullFetch)
      limitSQLString(Some("min"))
    else{
      splitSQL() match {
        case (sql,_) =>
          "select min(" + rcl.incrementColumn.name + ") from ( " + sql + " ) x_"
      }
    }
  }

  override def buildIncrementSQL(): String = {
    if(isFullFetch)
      limitSQLString()
    else {
      splitSQL() match {
        case (sql,_) =>
          val sqlBuilder = new StringBuilder()
            .append("select " + selects + " from (").append(sql).append(" ) x_  where ")
            .append(rcl.incrementColumn.name).append(">?").append(" and ").
            append(rcl.incrementColumn.name).append("<=?").
            append(" order by ").append(rcl.incrementColumn.name).append(" asc")
          //增量数据的Sql
          sqlBuilder.toString()
      }
    }
  }
  protected def splitSQL():(String,Option[String])={
    val sqls = resourceDefinition.sync.jdbc.sql.split(";")
    if(sqls.length == 2){
      (sqls(0),Some(sqls(1)))
    }else{
      (sqls(0),None)
    }
  }
  protected def limitSQLString(valueQuery:Option[String]=None):String
}
class OracleDataFetcher(rcl:ResourceConfigLike) extends BaseDataFetcher(rcl){
  //=== oracle implements from org.hibernate.dialect.Oracle8iDialect
  override protected def limitSQLString(valueQuery: Option[String]): String = {

    val pagingSelect = new StringBuffer(fullQuerySQL.length() + 100)

    valueQuery match {
      case Some(str) =>
        pagingSelect.append("select ").append(str).append("(rownum) from ( ")
      case None =>
        pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ")
    }

    pagingSelect.append(fullQuerySQL)

    valueQuery match {
      case Some(str) =>
        pagingSelect.append(" ) ")
      case None =>
        pagingSelect.append(" ) row_ ) where rownum_ > ? and rownum_ <= ?")
    }

    pagingSelect.toString
  }
}
class MySQLDataFetcher(rcl:ResourceConfigLike) extends BaseDataFetcher(rcl){
  override protected def limitSQLString(valueQuery: Option[String]): String = {
    valueQuery match {
      case Some(str) =>
        //FIXME 错误的查询方式
        "select " + str + " from (" + fullQuerySQL+ ") x_ limit ?,?"
      case None =>
        fullQuerySQL + " limit ?, ?"
    }
  }
}
class DB2DataFetcher(rcl:ResourceConfigLike) extends BaseDataFetcher(rcl){
  override protected def limitSQLString(valueQuery: Option[String]): String = {
    val startOfSelect = fullQuerySQL.toLowerCase.indexOf("select")

    val pagingSelect = new StringBuffer(fullQuerySQL.length() + 100)
      .append(fullQuerySQL.substring(0, startOfSelect)) // add the comment
      .append("select ")
    valueQuery match{
      case Some(str) =>
        pagingSelect.append(str).append("(rownumber_)")
      case None =>
        pagingSelect.append("*")
    }
    pagingSelect.append(" from ( select ") // nest the main query in an outer select
      .append(db2GetRowNumber(fullQuerySQL)); // add the rownnumber bit into the outer query select list

    if (db2HasDistinct(fullQuerySQL)) {
      pagingSelect.append(" row_.* from ( ") // add another (inner) nested select
        .append(fullQuerySQL.substring(startOfSelect)) // add the main query
        .append(" ) as row_"); // close off the inner nested select
    } else {
      pagingSelect.append(fullQuerySQL.substring(startOfSelect + 6)); // add the main query
    }

    pagingSelect.append(" ) as temp_")

    //add the restriction to the outer select
    if (valueQuery.isEmpty)
      pagingSelect.append(" where rownumber_ between ?+1 and ?")

    pagingSelect.toString
  }
  private def db2GetRowNumber(sql: String): String = {
    val rownumber = new StringBuffer(50)
      .append("rownumber() over(")

    val orderByIndex = sql.toLowerCase.indexOf("order by")

    if (orderByIndex > 0 && !db2HasDistinct(sql)) {
      rownumber.append(sql.substring(orderByIndex))
    }

    rownumber.append(") as rownumber_,")

    rownumber.toString
  }

  //=== db2 implements from org.hibernate.dialect.DB2Dialect
  private def db2HasDistinct(sql: String): Boolean = {
    sql.toLowerCase.indexOf("select distinct") >= 0
  }
}
