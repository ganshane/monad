// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.face.model.types

import java.sql.ResultSet

import monad.face.model.ResourceDefinition.ResourceProperty

/**
 * clob类型的列
 * @author jcai
 */

object ClobColumnType extends ClobColumnType

class ClobColumnType extends StringColumnType {
  override def readValueFromJdbc(rs: ResultSet, index: Int, cd: ResourceProperty) = {
    val clob = rs.getClob(index)
    if (clob != null) Some(clob.getSubString(1, clob.length.intValue())) else None
  }
}
