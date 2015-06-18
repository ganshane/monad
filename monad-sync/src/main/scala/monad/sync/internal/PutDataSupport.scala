// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import com.google.gson.JsonObject
import monad.jni.services.gen._
import monad.support.MonadSupportConstants
import monad.support.services.{LoggerSupport, MonadException}

/**
 * data put support
 */
trait PutDataSupport {
  this: SyncNoSQLSupport with SyncConfigLike with LoggerSupport =>
  def put(key: String, json: JsonObject, command: DataCommandType, timestamp: Long): MonadStatus = {
    val keyBytes = key.getBytes(MonadSupportConstants.UTF8_ENCODING_CHARSET)

    val partitionMappingKey = new PartitionMappingKey(keyBytes)
    val partitionMappingData = nosql.Get(partitionMappingKey)
    partitionMappingKey.delete()

    var partitionId: Option[Short] = None
    var internalCommand = command
    var dataSeqOpt: Option[Int] = None

    if (partitionMappingData == null) {
      // partition mapping not found
      if (command == DataCommandType.DEL) {
        error("data not found for delete operation!")
        return MonadStatus.OK()
      }
      partitionId = Some(balanceServer().partition.id)
    } else {
      // using old partition mapping value
      val partitionMappingValue = new PartitionMappingValue(partitionMappingData)
      partitionId = Some(partitionMappingValue.PartitionId())
      if (command == DataCommandType.PUT) {
        //说明已经分配好了数据，则操作变为更新
        internalCommand = DataCommandType.UPDATE
      }
      //使用老的数据序列
      dataSeqOpt = Some(partitionMappingValue.DataSeq())
      partitionMappingValue.delete()
    }

    val partitionInfo = partitionInfoData.get(partitionId.get)
    if (partitionInfo == null) {
      throw new MonadException("partition info not found", MonadSyncExceptionCode.PARTITION_NOT_FOUND)
    }
    val binlogSeq = partitionInfo.binlogSeq
    val dataSequence = partitionInfo.dataSeq
    val partition = partitionInfo.partition


    val dataSeq = if (dataSeqOpt.isDefined) dataSeqOpt.get else dataSequence.incrementAndGet()
    val syncBinlogOptions = new SyncBinlogOptions()
    syncBinlogOptions.setCommand_type(internalCommand)
    syncBinlogOptions.setData_seq(dataSeq)
    syncBinlogOptions.setPartition_id(partition.id)
    syncBinlogOptions.setSeq(binlogSeq.incrementAndGet())
    syncBinlogOptions.setTimestamp(timestamp)

    val dataBytes = json.toString.getBytes(MonadSupportConstants.UTF8_ENCODING_CHARSET)
    val status = nosql.PutDataWithBinlog(keyBytes, dataBytes, syncBinlogOptions)
    if (!status.ok()) {
      binlogSeq.decrementAndGet()
      //当且仅当是PUT的时候，进行那个回滚
      if (internalCommand == DataCommandType.PUT) {
        dataSequence.decrementAndGet()
      }
    }else{

    }
    status
  }
}
