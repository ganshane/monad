// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.internal

import java.util.concurrent.atomic.AtomicBoolean

import com.google.gson.JsonObject
import com.lmax.disruptor.EventHandler
import monad.face.MonadFaceConstants
import monad.face.model.ColumnType
import monad.face.services.DataTypeUtils
import monad.jni.services.gen.DataCommandType
import monad.support.MonadSupportConstants
import monad.support.services.MonadException
import monad.sync.model.DataEvent
import monad.sync.services.ResourceImporterManager

import scala.collection.JavaConversions._

/**
 * 保存记录的操作
 * @author jcai
 */
class SaveRecordHandler(manager: ResourceImporterManager) extends EventHandler[DataEvent] {
  private val threadNameFlag = new AtomicBoolean(false)

  def onEvent(event: DataEvent, sequence: Long, endOfBatch: Boolean) {
    if (threadNameFlag.compareAndSet(false, true)) {
      Thread.currentThread().setName("monad-background-SaveRecord")
    }
    val importer = manager.getObject(event.resourceName)
    if (importer == null) {
      throw new MonadException("通过%s未能找到对应的saver".format(event.resourceName), MonadSyncExceptionCode.SAVER_NOT_FOUND)
    }
    val row = event.row
    val timestamp = event.timestamp

    val json = new JsonObject
    var primaryKey: String = null
    var objectId: Array[Byte] = null
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
      //分析ID字段
      if ((col.mark & 8) == 8) {
        objectId = String.valueOf(fv).getBytes(MonadSupportConstants.UTF8_ENCODING_CHARSET)
      }
    }
    if (primaryKey == null) {
      throw new MonadException("[%s]主键字段为空".format(event.resourceName), MonadSyncExceptionCode.PRIMARY_KEY_VALUE_IS_NULL)
    }

    json.addProperty(MonadFaceConstants.UPDATE_TIME_FIELD_NAME, DataTypeUtils.convertDateAsInt(System.currentTimeMillis()))

    val status = importer.put(primaryKey, json, DataCommandType.PUT, timestamp)
    if (!status.ok())
      throw new MonadException("[%s] fail save data with status:%s".format(event.resourceName, status.toString), MonadSyncExceptionCode.FAIL_SAVE_DATA)
  }
}
