// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model.types

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.text.SimpleDateFormat
import java.util.Date

import com.google.gson.JsonObject
import monad.face.model.MonadColumnType
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.services.DataTypeUtils
import org.apache.lucene.document.{Field, NumericDocValuesField}
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * string column type
 * @author jcai
 * @version 0.1
 */
object DateColumnType extends DateColumnType

class DateColumnType extends MonadColumnType[Long] {
  private final val DEFAULT_DATE_FORMAT = "yyyyMMddHHmmss"

  def readValueFromJdbc(rs: ResultSet, index: Int, cd: ResourceProperty): Option[Long] = {
    if (!InternalUtils.isBlank(cd.dbFormat)) {
      val value = rs.getString(index)
      if (!InternalUtils.isBlank(value)) {
        val formatter = new SimpleDateFormat(cd.dbFormat)
        Some(formatter.parse(value.trim()).getTime)
      } else None
    } else {
      val value = rs.getTimestamp(index)
      if (value == null) None else Some(value.getTime)
    }
  }

  def readValueFromDfs(row: JsonObject, cd: ResourceProperty) = {
    if (row.has(cd.name)) Some(row.get(cd.name).getAsLong) else None
  }

  def convertDfsValueToString(value: Option[Long], cd: ResourceProperty): Option[String] = {
    if (value.isDefined) {
      val format = if (cd.apiFormat == null) DEFAULT_DATE_FORMAT else cd.apiFormat
      val formatter = new SimpleDateFormat(format)
      Some(formatter.format(new Date(value.get)))
    } else None
  }

  def setJdbcParameter(ps: PreparedStatement, i: Int, obj: Long, cd: ResourceProperty) {
    if (!InternalUtils.isBlank(cd.dbFormat)) {
      val formatter = new SimpleDateFormat(cd.dbFormat)
      ps.setString(i, formatter.format(new Date(obj)))
    } else {
      ps.setTimestamp(i, new Timestamp(obj))
    }
  }

  def createIndexField(value: Long, cd: ResourceProperty) = {
    val valueConverted = DataTypeUtils.convertDateAsInt(value)
    new NumericDocValuesField(cd.name, valueConverted)
  }

  def setIndexValue(f: Field, value: Long, cd: ResourceProperty) {
    f.setLongValue(DataTypeUtils.convertDateAsInt(value))
  }
}