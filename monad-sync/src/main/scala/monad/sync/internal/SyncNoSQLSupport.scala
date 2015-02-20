// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import java.io.File

import monad.jni.services.gen._
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.support.services.LoggerSupport
import org.apache.commons.io.FileUtils

/**
 * implements sync nosql service
 */
trait SyncNoSQLSupport
  extends FetchSyncDataSupport
  with LoadBalanceSupport
  with PutDataSupport {
  this: LoggerSupport with ResourceConfigLike =>
  //global nosql instance
  private[internal] var syncNoSQL: Option[SyncNoSQL] = None

  /**
   * fetch sync data for processor
   * @param request request
   * @return sync response
   */
  def fetchSyncData(request: SyncRequest): SyncResponse.Builder = {
    doFetchSyncData(request, 0)
  }

  def findMaxTimestamp() = {
    val dataTimestampKey = new DataTimestampKey(DataType.KV.swigValue())
    val bytes = nosql.Get(dataTimestampKey)
    var timestamp = 0L
    if (bytes != null) {
      val dataTimestampValue = new DataTimestampValue(bytes)
      timestamp = dataTimestampValue.Timestamp()
      dataTimestampValue.delete()
    }
    dataTimestampKey.delete()

    if (timestamp == 0) None else Some(timestamp)
  }

  protected def nosql: SyncNoSQL = syncNoSQL.get

  /**
   * 服务关闭
   */
  def shutdownNoSQL(): Unit = {
    info("shutdown sync nosql instance")
    if (syncNoSQL.isDefined)
      syncNoSQL.get.delete()
  }

  /**
   * 启动服务
   */
  def startNoSQL(): Unit = {
    info("start sync nosql instance")
    val noSQLOptions = new NoSQLOptions()
    noSQLOptions.setMax_open_files(config.sync.noSql.maxOpenFiles)
    noSQLOptions.setCache_size_mb(config.sync.noSql.cache)
    noSQLOptions.setBlock_size_kb(config.sync.noSql.blockSizeKb)
    noSQLOptions.setTarget_file_size(config.sync.noSql.targetFileSize)
    noSQLOptions.setMax_mmap_size(config.sync.noSql.maxMmapSize)
    noSQLOptions.setLog_keeped_num(1000)
    noSQLOptions.setWrite_buffer_mb(config.sync.noSql.writeBuffer)
    syncNoSQL = Some(new SyncNoSQL(config.sync.noSql.path + "/" + resourceDefinition.name, noSQLOptions))
    noSQLOptions.delete()
  }

  def destroyNoSQL() {
    val resourceName = resourceDefinition.name
    info("[{}] remove sync nosql database file", resourceName)
    val file = new File(config.sync.noSql.path + "/" + resourceName)
    val bakFile = new File(config.sync.noSql.path + "/" + resourceName + ".tmp_" + System.currentTimeMillis())
    FileUtils.moveDirectory(file, bakFile)
    FileUtils.deleteDirectory(bakFile)
  }
}

