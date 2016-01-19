// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.atomic.AtomicBoolean

import com.lmax.disruptor.EventHandler
import monad.face.MonadFaceConstants
import monad.face.model.IndexEvent
import monad.face.services.DocumentSource
import monad.jni.services.gen.DataCommandType
import monad.node.services.ResourceIndexerManager
import org.apache.lucene.document.Document
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * 创建文档
 * @author jcai
 */
class CreateDocumentHandler(resourceIndexerManager: ResourceIndexerManager, documentSource: DocumentSource) extends EventHandler[IndexEvent] {
  private val threadNameFlag = new AtomicBoolean(false)
  private val logger = LoggerFactory getLogger getClass

  def onEvent(event: IndexEvent, sequence: Long, endOfBatch: Boolean) {
    if (threadNameFlag.compareAndSet(false, true)) {
      Thread.currentThread().setName("monad-background-CreateIndex")
    }
    val indexerOpt = resourceIndexerManager.directGetObject(event.resource.name)
    indexerOpt match{
      case Some(indexer) =>
        if (event.commitFlag) {
          indexer.commit(event.commitSeq, event.version)
          event.reset()
          return
        }
        if ((sequence & MonadFaceConstants.NUM_OF_NEED_COMMIT) == 0) {
          logger.info("{} index event processed", sequence)
        }

        try {
          val rowId = event.id
          val command = DataCommandType.swigToEnum(event.command)
          def createDocument: Document = {
            try {
              documentSource.newDocument(event)
            } catch {
              case e: Throwable =>
                //FIXES #92 避免创建document失败导致一直在等待
                indexer.decIndexActionRef()
                throw e
            }
          }
          command match {
            case DataCommandType.PUT =>
              val doc = createDocument
              indexer.indexDocument(doc, event.version)
            case DataCommandType.UPDATE =>
              val doc = createDocument
              indexer.updateDocument(rowId, doc, event.version)
            case DataCommandType.DEL =>
              indexer.deleteDocument(rowId, event.version)
            case other =>
              throw new IllegalStateException("command not found")
          }
        } catch {
          case NonFatal(e) =>
            logger.error("[" + event.resource.name + "] fail to index", e)
        }
      case None=>
        logger.error("[" + event.resource.name + "] indexer not found")
    }
    //删除对对象的依赖
    event.reset()
  }
}
