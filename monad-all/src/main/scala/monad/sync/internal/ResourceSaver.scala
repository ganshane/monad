package monad.sync.internal

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.google.gson.JsonObject
import monad.core.services.GroupZookeeperTemplate
import monad.face.MonadFaceConstants
import monad.face.config.SyncConfigSupport
import monad.face.model.{ColumnType, ResourceDefinition}
import monad.face.services.DataTypeUtils
import monad.jni._
import monad.support.MonadSupportConstants
import monad.support.services.MonadException
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * resource saver
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class ResourceSaver(config: SyncConfigSupport,
                    rd: ResourceDefinition,
                    version: Int,
                    zk: GroupZookeeperTemplate,
                    delegate: Option[String => Option[ResourceSaver]] = None,
                    idNosql: Option[SyncIdNoSQL] = None) {
  private final val DEFAULT_TIMESTAMP_FORMATTER = "yyyy-MM-dd HH:mm:ss"
  private val logger = LoggerFactory getLogger getClass
  private[internal] var nosql: Option[SyncNoSQL] = None
  private var num = 0
  private var hasObjectIdColumn: Boolean = false

  def start() {
    val nosqlOptions = new NoSQLOptions()
    nosqlOptions.setCache_size_mb(config.sync.noSql.cache)
    nosqlOptions.setWrite_buffer_mb(config.sync.noSql.writeBuffer)
    nosqlOptions.setMax_open_files(config.sync.noSql.maxOpenFiles)
    nosqlOptions.setLog_keeped_num(config.sync.binlogLength)
    try {
      nosql = Some(new SyncNoSQL(config.sync.noSql.path + "/" + rd.name, nosqlOptions))
      config.sync.nodes.foreach(x => nosql.get.AddRegion(x.id, x.weight))
      if (idNosql.isDefined)
        nosql.get.SetSyncIdNoSQL(idNosql.get)
    } finally {
      nosqlOptions.delete()
    }
    logger.info("[{}] nosql started", rd.name)

    rd.properties.foreach { pro =>
      if ((pro.mark & 8) == 8) {
        if (hasObjectIdColumn) {
          throw new MonadException("重复定义对象ID列", MonadSyncExceptionCode.DUPLICATE_OBJECT_ID_COLUMN)
        }
        hasObjectIdColumn = true
      }
    }
  }

  def destroy() {
    stop()
    logger.info("[{}] remove sync nosql database file", rd.name)
    val file = new File(config.sync.noSql.path + "/" + rd.name)
    val bakFile = new File(config.sync.noSql.path + "/" + rd.name + ".tmp")
    FileUtils.moveDirectory(file, bakFile)
    FileUtils.deleteDirectory(bakFile)
  }

  def stop() {
    if (nosql.isDefined) {
      nosql.get.delete()
      nosql = None
    }
  }

  def findMaxValue: Option[Long] = {
    val bytes = nosql.get.RawGet(CMonadConstants.DATA_MAX_TIMESTAMP.getBytes(MonadSupportConstants.UTF8_ENCODING_CHARSET))
    if (bytes != null) {
      return DataTypeUtils.convertAsLong(Some(bytes))
    }
    None
  }

  def save(row: Array[Any], timestamp: Long, rowVersion: Int) {
    if (version != -1 && version != rowVersion) {
      logger.warn("[" + rd.name + "] saver version {} != data version {}", version, rowVersion)
      return
    }
    if (row == null) {
      //仅仅更新信息
      outputRegionInfo(timestamp)
      return
    }
    delegate match {
      case Some(finder) => //如果有代理的saver，则调用代理的类
        val innerSaverOpt = finder.apply(rd.targetResource)
        val innerSaver = innerSaverOpt.getOrElse {
          throw new MonadException("资源[%s]定义的目标资源[%s]对应Saver不存在".format(rd.name, rd.targetResource), MonadSyncExceptionCode.TARGET_RESOURCE_NOT_EXIST)
        }

        innerSaver.save(row, timestamp, -1) // -1 的情况忽略版本检查
      //更新本资源的timestamp
      val status = nosql.get.UpdateDataTimestamp(timestamp)
        if (status != StatusType.S_OK)
          throw new MonadException(MonadSyncExceptionCode.FAIL_UPDATE_TIMESTAMP)
      case None =>
        internalSave(row, timestamp)
    }
    num += 1
    if ((num & MonadFaceConstants.NUM_OF_NEED_UPDATE_REGION_INFO) == 0) {
      outputRegionInfo(timestamp)
    }
  }

  def outputRegionInfo(timestamp: Long) {
    val formatter = new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMATTER)
    val jsonObject = new JsonObject
    val nosqlRegionInfo = new String(nosql.get.GetRegionDataInfo())
    try {
      val regionInfo = MonadFaceConstants.GLOBAL_GSON_PARSER.parse(nosqlRegionInfo).getAsJsonArray
      jsonObject.add("region_data", regionInfo)
    } catch {
      case e: Throwable =>
        logger.warn("[{" + rd.name + "}] fail to to parse " + nosqlRegionInfo, e)
    }
    jsonObject.addProperty("data_count", nosql.get.GetDataStatCount())
    jsonObject.addProperty("time_stamp", formatter.format(new Date(timestamp)) + "(" + timestamp + ")")
    zk.setRegionSyncInfo(rd.name, jsonObject)
  }

  def internalSave(row: Array[Any], timestamp: Long) {
    val json = new JsonObject
    var primaryKey: String = null
    var objectId: Array[Byte] = null
    for ((col, fv) <- rd.properties.view.zip(row) if fv != null) {
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
      throw new MonadException("[%s]主键字段为空".format(rd.name), MonadSyncExceptionCode.PRIMARY_KEY_VALUE_IS_NULL)
    }
    if (hasObjectIdColumn && objectId == null) {
      throw new MonadException("[%s]objectId字段为空".format(rd.name), MonadSyncExceptionCode.OBJECT_ID_IS_NULL)
    }

    json.addProperty(MonadFaceConstants.UPDATE_TIME_FIELD_NAME, DataTypeUtils.convertDateAsInt(System.currentTimeMillis()))

    val status = nosql.get.Put2(primaryKey.getBytes(MonadSupportConstants.UTF8_ENCODING_CHARSET),
      json.toString.getBytes(MonadSupportConstants.UTF8_ENCODING),
      timestamp, objectId)
    if (status != StatusType.S_OK)
      throw new MonadException("[%s] fail save data with status:%s".format(rd.name, status.toString), MonadSyncExceptionCode.FAIL_SAVE_DATA)
  }
}
