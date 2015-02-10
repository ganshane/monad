// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import java.util.concurrent.ConcurrentHashMap

import com.google.protobuf.ByteString
import monad.core.services.FetchSyncDataSupport.NoSQLGetter
import monad.jni.services.JNIErrorCode
import monad.jni.services.gen._
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.support.services.{MonadException, LoggerSupport}

import scala.annotation.tailrec


/**
 * 抓取同步数据
 */
object FetchSyncDataSupport {
  type NoSQLGetter = {
    def nosql: SyncNoSQL
  }
}

trait FetchSyncDataSupport {
  this: NoSQLGetter with LoggerSupport =>
  private val minSeqMap = new ConcurrentHashMap[Short, Long]()

  def doFetchSyncData(request: SyncRequest, logKeptNum: Int = 0): SyncResponse = {
    val partitionId = request.getPartitionId.toShort
    val startLog = request.getLogSeqFrom
    val size = request.getSize
    debug("sync request received partition:" + partitionId + " startLog:{} size:{}"
      , startLog, size)
    val syncResponseBuilder = SyncResponse.newBuilder()
    syncResponseBuilder.setPartitionId(partitionId)

    @tailrec
    def createBinlogData(log: Long, until: Long): Unit = {
      if (log < until) {
        val logKey = new SyncBinlogKey(partitionId, log)
        val logData = GetBinlogValue(logKey)
        logKey.delete()
        if (logData == null) {
          //TODO 是否进行跳号处理？
          //warn("binlog {} not found for partition id:{}",log,partitionId)
          logKey.delete()
        } else {
          val responseDataBuilder = syncResponseBuilder.addResponseDataBuilder()
          responseDataBuilder.setBinlogValue(ByteString.copyFrom(logData))
          createBinlogData(log + 1, until)
        }
      }
    }

    createBinlogData(startLog, startLog + size)

    /*
    breakable {
      var logKey: SyncBinlogKey = null
      startLog until (startLog + size) foreach { case log =>
        try {
          logKey = new SyncBinlogKey(partitionId, log)
          val logData = GetBinlogValue(logKey)
          if (logData == null) {
            //TODO 是否进行跳号处理？
            //warn("binlog {} not found for partition id:{}",log,partitionId)
            logKey.delete()
            break()
          }
          val responseDataBuilder = syncResponseBuilder.addResponseDataBuilder()
          responseDataBuilder.setBinlogValue(ByteString.copyFrom(logData))
        } finally {
          if (logKey != null)
            logKey.delete()
        }
      }
    }
    */
    if (logKeptNum > 0) {
      //需要保存的log日志
      val toSeq = startLog - logKeptNum - 1
      if (toSeq > 0) {
        val begin = findMinBinlogSeqByPartitionId(partitionId)
        if (begin < toSeq && (toSeq - begin) > logKeptNum) {
          //一次删除10万条
          info("delete binlog from {} to {} for " + partitionId, begin, toSeq)
          nosql.DeleteBinlogRange(partitionId, begin, toSeq)
          updateMinBinlogSeq(partitionId, toSeq)
        }
      }
    }

    syncResponseBuilder.build()
  }

  private def findMinBinlogSeqByPartitionId(partitionId: Short): Long = {
    var result = minSeqMap.get(partitionId)
    if (result == 0) {
      minSeqMap.putIfAbsent(partitionId, nosql.FindMinBinlogSeqByPartitionId(partitionId))
      result = minSeqMap.get(partitionId)
    }
    result
  }

  def GetBinlogValue(key: SyncBinlogKey): Array[Byte] = {
    val binlogValue = new SyncBinlogValue
    val status = nosql.GetBinlogValue(key,binlogValue)
    if(status.ok())
      binlogValue.GetValue()
    else
      throw new MonadException(status.GetState(),JNIErrorCode.JNI_STATUS_ERROR)
  }

  private def updateMinBinlogSeq(partitionId: Short, toSeq: Long): Unit = {
    minSeqMap.put(partitionId, toSeq + 1)
  }
}
