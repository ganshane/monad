// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model.types

import java.sql.{PreparedStatement, ResultSet}

import com.google.gson.JsonObject
import monad.face.model.MonadColumnType
import monad.face.model.ResourceDefinition.ResourceProperty
import org.apache.lucene.document.{Field, LongField, NumericDocValuesField}

/**
 * Long Type Column
 * @author jcai
 * @version 0.1
 */

object LongColumnType extends LongColumnType

class LongColumnType extends MonadColumnType[Long] {
  def readValueFromJdbc(rs: ResultSet, index: Int, cd: ResourceProperty) = {
    val value = rs.getBigDecimal(index)
    if (value == null) None else Some(value.longValue())
  }

  def readValueFromDfs(row: JsonObject, cd: ResourceProperty) = {
    if (row.has(cd.name)) Some(row.get(cd.name).getAsLong) else None
  }

  def convertDfsValueToString(value: Option[Long], cd: ResourceProperty) =
    if (value.isDefined) Some(value.get.toString) else None

  def setJdbcParameter(ps: PreparedStatement, index: Int, value: Long, cd: ResourceProperty) {
    //oralce使用bigDecimal
    //ps.setLong(index, value)
    ps.setBigDecimal(index, new java.math.BigDecimal(value))
  }

  def createIndexField(value: Long, cd: ResourceProperty) =
    (new LongField(cd.name, value, LongField.TYPE_NOT_STORED),Some(new NumericDocValuesField(cd.name,value)))

  def setIndexValue(f: (Field,Option[Field]), value: Long, cd: ResourceProperty) {
    f._1.setLongValue(value)
    f._2.foreach(_.setLongValue(value))
  }
}