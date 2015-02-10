// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import java.util.concurrent.Semaphore

import monad.jni.services.JNIErrorCode
import monad.jni.services.gen.{SlaveNoSQLSupport, SyncBinlogValue}
import monad.protocol.internal.CommandProto.BaseCommand
import monad.protocol.internal.InternalSyncProto
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.rpc.services._
import monad.support.services.{LoggerSupport, MonadException}
import org.apache.tapestry5.ioc.services.cron.{PeriodicExecutor, PeriodicJob}
import org.jboss.netty.channel.{Channel, ChannelFuture, ChannelFutureListener}

/**
 * spout data synchronizer
 */
trait DataSynchronizer {
  def sendSyncRequest(channelOpt: Option[Channel]): Option[ChannelFuture]

  def processSyncData(response: InternalSyncProto.SyncResponse): Boolean

  def getPartitionData: Array[Short]

  def findNoSQLByPartitionId(partitionId: Short): Option[SlaveNoSQLSupport]

  def unlock(channel: Channel)
}

trait DataSynchronizerSupport
  extends DataSynchronizer
  with ProtobufCommandHelper {
  this: LoggerSupport =>
  private final val thirtySeconds = 30 * 1000
  private val ONE_BATCH_SIZE = 1000
  private val semaphore = new Semaphore(1)
  private val closeListener = new ChannelFutureListener {
    override def operationComplete(future: ChannelFuture): Unit = {
      semaphore.release()
    }
  }
  private var syncJob: Option[PeriodicJob] = None
  @volatile
  private var processTime: Long = 0
  private var partitionIndex = 0
  private var partitions: Array[Short] = _
  private var rpcClient: RpcClient = _
  private var masterMachinePath: String = _

  override def unlock(channel: Channel): Unit = {
    try {
      channel.getCloseFuture.removeListener(closeListener)
    } finally {
      semaphore.release()
    }
  }

  def startSynchronizer(masterMachinePath: String, periodicExecutor: PeriodicExecutor, rpcClient: RpcClient) {
    this.masterMachinePath = masterMachinePath

    //初始化分区信息
    partitions = getPartitionData
    this.rpcClient = rpcClient
    val job = periodicExecutor.addJob(new CronScheduleWithStartModel("0/5 * * * * ? *", StartAtDelay), "sync-database-for-processor", new Runnable {
      override def run(): Unit = {
        val timeDiff = System.currentTimeMillis() - processTime
        partitionIndex = 0
        if (timeDiff > thirtySeconds) {
          warn("no response data from meta server,time:{}", timeDiff)
        }
        if (semaphore.tryAcquire()) {
          try {
            info("begin to sync partition:{}", partitions(0))
            val future = sendSyncRequest(None)
            if (future.isEmpty) {
              //没有写入成功
              semaphore.release()
            } else {
              future.get.getChannel.getCloseFuture.addListener(closeListener)
            }
          } catch {
            case e: Throwable => //发生异常则释放lock
              semaphore.release()
              error(e.getMessage, e)
          }
        }
      }
    })
    if (job != null)
      syncJob = Some(job)
  }

  def sendSyncRequest(channelOpt: Option[Channel]): Option[ChannelFuture] = {
    processTime = System.currentTimeMillis()
    val message = wrap(SyncRequest.cmd, constructSyncRequest)
    channelOpt match {
      case Some(channel) =>
        Some(channel.write(message))
      case None =>
        rpcClient.writeMessage(masterMachinePath, message)
    }
  }

  private[monad] def constructSyncRequest = {
    val partitionId = partitions(partitionIndex)
    val builder = SyncRequest.newBuilder()
    builder.setPartitionId(partitionId)
    val nosqlOpt = findNoSQLByPartitionId(partitionId)
    builder.setLogSeqFrom(nosqlOpt.get.FindLastBinlog() + 1)
    builder.setSize(ONE_BATCH_SIZE)
    builder.build()
  }

  /**
   * process response from sync server
   */
  def processSyncData(response: InternalSyncProto.SyncResponse): Boolean = {
    val nosqlOpt = findNoSQLByPartitionId(response.getPartitionId.toShort)
    nosqlOpt match {
      case Some(nosql) =>
        val nosql = nosqlOpt.get
        val it = response.getResponseDataList.iterator()
        while (it.hasNext) {
          val syncData = it.next()
          val binlogValue = new SyncBinlogValue(syncData.getBinlogValue.toByteArray)
          val status = nosql.PutBinlog(binlogValue)
          if (!status.ok()) {
            status.delete()
            throw new MonadException(new String(status.ToString()), JNIErrorCode.JNI_STATUS_ERROR)
          }
          status.delete()
        }
        info("{} row processed.", response.getResponseDataCount)
        isContinue(response)
      case None =>
        warn("nosql not found by partition id[{}]", response.getPartitionId)
        false
    }
  }

  protected def isContinue(response: SyncResponse): Boolean = {
    val currentPartitionCompleted = response.getResponseDataCount < ONE_BATCH_SIZE
    if (currentPartitionCompleted) {
      info("finish sync partition[{}] data", response.getPartitionId)
      partitionIndex += 1
      val r = partitionIndex < partitions.length //未超出所有分区
      if (r)
        info("begin to sync partition:{}", partitions(partitionIndex))

      return r
    }
    !currentPartitionCompleted
  }

  /**
   * 服务关闭
   */
  def shutdownSynchronizer(): Unit = {
    syncJob.foreach(_.cancel())
  }
}

/**
 * message received filter
 */
class DataSyncMessageFilter(ds: DataSynchronizer)
  extends RpcClientMessageFilter
  with LoggerSupport {
  /**
   * handle rpc client message
   * @param command base command
   * @return
   */
  override def handle(command: BaseCommand, channel: Channel, handler: RpcClientMessageHandler): Boolean = {
    if (!command.hasExtension(SyncResponse.cmd))
      return handler.handle(command, channel)

    var isContinue = false
    try {
      isContinue = ds.processSyncData(command.getExtension(SyncResponse.cmd))
    } finally {
      if (isContinue) {
        ds.sendSyncRequest(Some(channel))
      } else {
        ds.unlock(channel)
      }
    }
    true
  }
}
