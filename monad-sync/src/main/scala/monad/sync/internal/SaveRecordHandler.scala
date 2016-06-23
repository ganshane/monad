// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import java.util.concurrent.atomic.AtomicBoolean

import com.google.gson.JsonObject
import com.lmax.disruptor.EventHandler
import monad.face.MonadFaceConstants
import monad.face.model.ColumnType
import monad.face.services.DataTypeUtils
import monad.jni.services.gen.DataCommandType
import stark.utils.services.{LoggerSupport, StarkException}
import monad.sync.model.DataEvent
import monad.sync.services.ResourceImporterManager

import scala.collection.JavaConversions._

/**
 * 保存记录的操作
 * @author jcai
 */
class SaveRecordHandler(manager: ResourceImporterManager)
  extends EventHandler[DataEvent]
  with LoggerSupport {
  private val threadNameFlag = new AtomicBoolean(false)

  def onEvent(event: DataEvent, sequence: Long, endOfBatch: Boolean) {
    if (threadNameFlag.compareAndSet(false, true)) {
      Thread.currentThread().setName("monad-background-SaveRecord")
    }
    val importer = manager.getObject(event.resourceName)
    if (importer == null) {
      throw new StarkException("通过%s未能找到对应的saver".format(event.resourceName), MonadSyncExceptionCode.SAVER_NOT_FOUND)
    }
    val row = event.row
    val timestamp = event.timestamp

    if (row == null) {
      //TODO 写入资源信息到zk中
      return
    }

    val json = new JsonObject
    var primaryKey: String = null
    for ((col, fv) <- importer.rd.properties.view.zip(row) if fv != null) {
      col.columnType match {
        case ColumnType.Date | ColumnType.Long | ColumnType.Int =>
          json.addProperty(col.name, fv.asInstanceOf[Number])
        case ColumnType.Clob | ColumnType.String =>
          json.addProperty(col.name, fv.asInstanceOf[String])
      }

      if (col.primaryKey) {
        primaryKey = String.valueOf(fv)
      }
    }
    if (primaryKey == null) {
      throw new StarkException("[%s]主键字段为空".format(event.resourceName), MonadSyncExceptionCode.PRIMARY_KEY_VALUE_IS_NULL)
    }

    json.addProperty(MonadFaceConstants.UPDATE_TIME_FIELD_NAME, DataTypeUtils.convertDateAsInt(System.currentTimeMillis()))

    event.objectId.foreach{ord=>
      json.addProperty(MonadFaceConstants.OID_FILED_NAME, ord)
    }

    val status = importer.put(primaryKey, json, DataCommandType.PUT, timestamp)
    if (!status.ok())
      throw new StarkException("[%s] fail save data with status:%s".format(event.resourceName, new String(status.ToString())), MonadSyncExceptionCode.FAIL_SAVE_DATA)

    if ((sequence & MonadFaceConstants.NUM_OF_NEED_COMMIT) == 0) {
      info("{} records saved", sequence)
    }
  }
}
