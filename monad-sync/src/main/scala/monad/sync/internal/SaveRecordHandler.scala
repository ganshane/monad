// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.internal

import java.util.concurrent.atomic.AtomicBoolean

import com.lmax.disruptor.EventHandler
import monad.face.MonadFaceConstants
import monad.support.services.MonadException
import monad.sync.model.DataEvent
import org.slf4j.LoggerFactory

/**
 * 保存记录的操作
 * @author jcai
 */
class SaveRecordHandler(syncServer: ResourceSyncServer) extends EventHandler[DataEvent] {
  private final val logger = LoggerFactory getLogger getClass
  private val threadNameFlag = new AtomicBoolean(false)

  def onEvent(event: DataEvent, sequence: Long, endOfBatch: Boolean) {
    if (threadNameFlag.compareAndSet(false, true)) {
      Thread.currentThread().setName("monad-background-SaveRecord")
    }
    val saverOpt = syncServer.getSaver(event.resourceName)
    saverOpt match {
      case Some(saver) =>
        saver.save(event.row, event.timestamp, event.version)
        if ((sequence & MonadFaceConstants.NUM_OF_NEED_COMMIT) == 0) {
          logger.info("{} row saved.", sequence)
        }
      case _ =>
        throw new MonadException("通过%s未能找到对应的saver".format(event.resourceName), MonadSyncExceptionCode.SAVER_NOT_FOUND)
    }
  }
}
