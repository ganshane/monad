// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.ConcurrentHashMap

import monad.face.MonadFaceConstants
import monad.face.model.IndexEvent
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.services.ResourceDefinitionConversions._
import monad.face.services.{DocumentCreator, DocumentSource}
import org.apache.lucene.document.{Document, Field, _}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * implements document source
 * @author jcai
 */
class DocumentSourceImpl(factories: java.util.Map[String, DocumentCreator]) extends DocumentSource {
  //sid field
  private val sidField = new NumericDocValuesField(MonadFaceConstants.OBJECT_ID_PAYLOAD_FIELD, 0)
  private val oidField = new NumericDocValuesField(MonadFaceConstants.OID_FILED_NAME, 0)
  private val logger = LoggerFactory getLogger getClass
  private val cacheCreator = new ConcurrentHashMap[String, DocumentCreator]()
  private val idField = new IntField(MonadFaceConstants.OBJECT_ID_FIELD_NAME, 1, IntField.TYPE_NOT_STORED)
  private val utField = new IntField(MonadFaceConstants.UPDATE_TIME_FIELD_NAME, 1, IntField.TYPE_NOT_STORED)

  def newDocument(event: IndexEvent) = {
    //优先使用自定义的DocumentCreator
    var creator = factories.get(event.resource.name)

    //如果发现自定义为空，则进行创建默认DocumentCreator
    if (creator == null) {
      creator = cacheCreator.get(event.resource.name)
      if (creator == null) {
        var analyticsIdSeq:Option[Int] = None
        for ((col, index) <- event.resource.properties.view.zipWithIndex) {
          if (col.objectCategory != null) {
            if (analyticsIdSeq.isDefined) {
              logger.warn("[{}] duplicate analytics id decleared", event.resource.name)
            }
            analyticsIdSeq = Some(index)
          }
        }
        val value = new DefaultDocumentCreator()
        creator = cacheCreator.putIfAbsent(event.resource.name, value)
        if (creator == null)
          creator = value
      }
    }
    val doc = creator.newDocument(event)
    //设置主键字段
    idField.setIntValue(event.id)
    doc.add(idField)

    if(event.row.has(MonadFaceConstants.OID_FILED_NAME)){
      val oid = event.row.get(MonadFaceConstants.OID_FILED_NAME).getAsInt
      oidField.setLongValue(oid)
      doc.add(oidField)
    }

    //用来快速更新
    sidField.setLongValue(event.id)
    doc.add(sidField)


    //设置更新时间
    if (event.row.has(MonadFaceConstants.UPDATE_TIME_FIELD_NAME)) {
      utField.setIntValue(event.row.get(MonadFaceConstants.UPDATE_TIME_FIELD_NAME).getAsInt)
      doc.add(utField)
    }

    doc
  }
}

class DefaultDocumentCreator extends DocumentCreator {
  private val cachedFields = scala.collection.mutable.Map[String, (Field,Option[Field])]()
  private var version = -1

  def newDocument(event: IndexEvent) = {
    if (version != event.version) {
      //检查一下version
      cachedFields.clear()
      version = event.version
    }
    val doc = new Document

    for ((col, index) <- event.resource.properties.view.zipWithIndex) {
      val valueOpt = col.readDfsValue(event.row)
      valueOpt match {
        case Some(value) =>
          val f = cachedFields.get(col.name)
          f match {
            case Some(field) =>
              setIndexValue(col, field, value)
              doc.add(field._1)
              //添加排序
              field._2.foreach(doc.add)
            case None =>
              val field = createFieldable(col, value)
              cachedFields.put(col.name, field)
              doc.add(field._1)
              //添加排序
              field._2.foreach(doc.add)
          }
        case _ =>
        //
      }
    }

    doc
  }

  protected def createFieldable(col: ResourceProperty, value: Any) = {
    col.createIndexField(value)
  }

  protected def setIndexValue(col: ResourceProperty, f: (Field,Option[Field]), value: Any) {
    col.setIndexValue(f, value)
  }
}

