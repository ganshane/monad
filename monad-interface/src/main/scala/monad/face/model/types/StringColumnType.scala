// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model.types

import java.sql.{ResultSet, PreparedStatement}
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.{TextField, StringField, Field}
import monad.face.model.MonadColumnType
import monad.face.model.ResourceDefinition.ResourceProperty
import com.google.gson.JsonObject
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * string column type
 * @author jcai
 * @version 0.1
 */
class StringColumnType extends MonadColumnType[String]{
    private final val GBK="GBK"
    def readValueFromJdbc(rs: ResultSet,index:Int,cd: ResourceProperty) = {
        val value =
        if(cd.resourceDefinition != null && cd.resourceDefinition.sync.encoding != null){
            new String(rs.getString(index).getBytes(cd.resourceDefinition.sync.encoding),GBK)
        }else{
            rs.getString(index)
        }
        if(InternalUtils.isBlank(value)) None else Some(value.trim)
    }

    def readValueFromDfs(row: JsonObject, cd: ResourceProperty) = {
        if(row.has(cd.name)) Some(row.get(cd.name).getAsString) else None
    }
    def convertDfsValueToString(value:Option[String],cd:ResourceProperty):Option[String]={
        value
    }
    def setJdbcParameter(ps: PreparedStatement, i: Int, obj: String,cd:ResourceProperty) {
        ps.setString(i,obj)
    }
    def createIndexField(value: String,cd:ResourceProperty) = {
        new Field(cd.name, value, Store.NO, cd.indexType.indexType())
    }
    def setIndexValue(f:Field,value:String,cd:ResourceProperty){
        f.asInstanceOf[Field].setStringValue(value)
    }
}