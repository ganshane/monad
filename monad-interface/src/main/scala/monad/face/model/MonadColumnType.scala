// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import java.sql.{PreparedStatement, ResultSet}

import com.google.gson.JsonObject
import monad.face.model.ResourceDefinition.ResourceProperty
import org.apache.lucene.document.Field


/**
 * 列类型接口
 * @author jcai
 * @version 0.1
 */
trait MonadColumnType[T] {
  /**
   * 通过jdbc获取数据
   * @param rs 结果集
   * @param index 结果集中的索引
   * @param cd 列定义
   */
  def readValueFromJdbc(rs:ResultSet,index:Int,cd:ResourceProperty):Option[T]

  /**
   * 从分布式数据库中获得数据
   * @param row 行数据
   * @param cd 对应列的定义
   */
  def readValueFromDfs(row:JsonObject,cd:ResourceProperty):Option[T]

  /**
   * 从分布式数据中读取数据用来供API显示
   * @param value 行数据
   * @param cd 对应列的定义
   */
  def convertDfsValueToString(value:Option[T],cd:ResourceProperty):Option[String]

  /**
   * 设置jdbc的参数
   * @param ps jdbc prepared statement
   * @param index ps index
   * @param value value
   * @param cd 列定义
   */
  def setJdbcParameter(ps:PreparedStatement,index:Int,value:T,cd:ResourceProperty)

  /**
   * 创建索引字段对象
   */
  def createIndexField(value: T,cd:ResourceProperty):Field

  def setIndexValue(f:Field,value:T,cd:ResourceProperty)
}