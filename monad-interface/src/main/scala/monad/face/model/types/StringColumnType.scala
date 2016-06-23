// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model.types

import java.sql.{PreparedStatement, ResultSet}

import com.google.gson.JsonObject
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.model.{AnalyzerCreator, IndexType, MonadColumnType}
import monad.face.services.MonadFaceExceptionCode
import stark.utils.services.StarkException
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.{Field, StringField, TextField}
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * string column type
 * @author jcai
 * @version 0.1
 */
object StringColumnType extends StringColumnType {
}

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
      val f = cd.indexType match {
        case IndexType.Keyword =>
          new StringField(cd.name, value, Store.NO)
        case IndexType.Text =>
          //TODO 此处频繁创建analyzer,
          if(cd.analyzer != null){
            val analyzer = AnalyzerCreator.create(cd.analyzer)
            new TextField(cd.name, analyzer.tokenStream(cd.name,value))
          }else{
            new TextField(cd.name, value, Store.NO)
          }
        case other =>
          throw new StarkException("index type %s unsupported".format(cd.indexType), MonadFaceExceptionCode.INDEX_TYPE_NOT_SUPPORTED)
      }
      (f,None)
    }
    def setIndexValue(f:(Field,Option[Field]),value:String,cd:ResourceProperty){
      //TODO 此处频繁创建analyzer,
      if(cd.analyzer != null) {
        val analyzer = AnalyzerCreator.create(cd.analyzer)
        f._1.asInstanceOf[Field].setTokenStream(analyzer.tokenStream(cd.name,value))
      }else {
        f._1.asInstanceOf[Field].setStringValue(value)
      }
    }
}