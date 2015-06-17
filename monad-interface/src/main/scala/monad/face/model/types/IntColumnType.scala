// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model.types

import java.sql.{PreparedStatement, ResultSet}

import com.google.gson.JsonObject
import monad.face.model.MonadColumnType
import monad.face.model.ResourceDefinition.ResourceProperty
import org.apache.lucene.document.{Field, NumericDocValuesField}

/**
 * Int Type Column
 * @author jcai
 * @version 0.1
 */

object IntColumnType extends IntColumnType

class IntColumnType extends MonadColumnType[Int] {
  def readValueFromJdbc(rs: ResultSet, index: Int, cd: ResourceProperty) = {
    val value = rs.getBigDecimal(index)
    if (value == null) None else Some(value.intValue())
  }

  def readValueFromDfs(row: JsonObject, cd: ResourceProperty) = {
    if (row.has(cd.name)) Some(row.get(cd.name).getAsInt) else None
  }

  def convertDfsValueToString(value: Option[Int], cd: ResourceProperty) =
    if (value.isDefined) Some(value.get.toString) else None

  def setJdbcParameter(ps: PreparedStatement, index: Int, value: Int, cd: ResourceProperty) {
    ps.setInt(index, value)
  }

  def createIndexField(value: Int, cd: ResourceProperty) =
    new NumericDocValuesField(cd.name,value)
    //new IntField(cd.name, value, IntField.TYPE_NOT_STORED)

  def setIndexValue(f: Field, value: Int, cd: ResourceProperty) {
    f.setLongValue(value)
  }
}