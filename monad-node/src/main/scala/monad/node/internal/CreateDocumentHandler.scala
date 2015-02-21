// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.atomic.AtomicBoolean

import com.lmax.disruptor.EventHandler
import monad.face.model.IndexEvent
import monad.face.services.DocumentSource
import monad.jni.services.gen.DataCommandType
import monad.node.services.ResourceIndexerManager
import org.slf4j.LoggerFactory

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
    val indexer = resourceIndexerManager.directGetObject(event.resource.name)
    if (event.commitFlag) {
      indexer.commit(event.commitSeq, event.version)
      event.reset()
      return
    }

    try {
      val rowId = event.id
      val command = DataCommandType.swigToEnum(event.command)
      command match {
        case DataCommandType.PUT =>
          val doc = documentSource.newDocument(event)
          indexer.indexDocument(doc, event.version)
        case DataCommandType.UPDATE =>
          val doc = documentSource.newDocument(event)
          indexer.updateDocument(rowId, doc, event.version)
        case DataCommandType.DEL =>
          indexer.deleteDocument(rowId, event.version)
        case other =>
          throw new IllegalStateException("command not found")
      }
    } catch {
      case e: Throwable =>
        logger.error("[" + event.resource.name + "] fail to index", e)
    }
    //删除对对象的依赖
    event.reset()
  }
}
