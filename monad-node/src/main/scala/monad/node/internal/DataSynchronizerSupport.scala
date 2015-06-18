// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import monad.core.services.{CronScheduleWithStartModel, StartAtDelay}
import monad.face.MonadFaceConstants
import monad.jni.services.JNIErrorCode
import monad.jni.services.gen.{SlaveNoSQLSupport, SyncBinlogValue}
import monad.node.services.MonadNodeExceptionCode
import monad.protocol.internal.InternalSyncProto
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.rpc.protocol.CommandProto.BaseCommand
import monad.rpc.services._
import monad.support.services.{LoggerSupport, MonadException}
import org.apache.tapestry5.ioc.services.cron.{PeriodicExecutor, PeriodicJob}
import org.jboss.netty.channel.{Channel, ChannelFuture, ChannelFutureListener}

import scala.annotation.tailrec
import scala.util.control.NonFatal

/**
 * spout data synchronizer
 */
trait DataSynchronizer {
  def sendSyncRequest(channelOpt: Option[Channel]): Option[ChannelFuture]

  def processSyncData(response: InternalSyncProto.SyncResponse): Boolean

  def getResourceList: Array[String]

  def getPartitionId: Short

  def findNoSQLByResourceName(resourceName: String): Option[SlaveNoSQLSupport]

  def unlock(channel: Channel)

  def afterFinishSync()
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
  private val processTime: AtomicLong = new AtomicLong(System.currentTimeMillis())
  private val afterDoing = new AtomicBoolean(false)
  private val totalData = new AtomicLong(0)
  private var syncJob: Option[PeriodicJob] = None
  private var resourceIndex = 0
  private var resources: Array[String] = _
  private var rpcClient: RpcClient = _
  private var masterMachinePath: String = _

  override def unlock(channel: Channel): Unit = {
    try {
      channel.getCloseFuture.removeListener(closeListener)
    } finally {
      finishRequest()
    }
  }

  def startSynchronizer(masterMachinePath: String, periodicExecutor: PeriodicExecutor, rpcClient: RpcClient) {
    this.masterMachinePath = masterMachinePath

    this.rpcClient = rpcClient
    val job = periodicExecutor.addJob(new CronScheduleWithStartModel("0/5 * * * * ? *", StartAtDelay), "sync-database-for-node", new Runnable {
      override def run(): Unit = {
        val timeDiff = System.currentTimeMillis() - processTime.get()
        resourceIndex = 0
        if (timeDiff > thirtySeconds && !afterDoing.get()) {
          warn("no response data from sync server,time:{}", timeDiff)
        }
        if (semaphore.tryAcquire()) {
          //初始化分区信息
          try {
            resources = getResourceList
            if (resources.length == 0) {
              finishRequest()
            } else {
              info("begin to sync resource:{}", resources(0))
              val futureOpt = sendSyncRequest(None)
              futureOpt match {
                case Some(future) =>
                  future.getChannel.getCloseFuture.addListener(closeListener)
                case None =>
                  finishRequest()
              }
            }
          } catch {
            case NonFatal(e) => //发生异常则释放lock
              error(e.getMessage, e)
              finishRequest()
          }
        }
      }
    })
    if (job != null)
      syncJob = Some(job)
  }

  def sendSyncRequest(channelOpt: Option[Channel]) = {
    processTime.set(System.currentTimeMillis())
    val message = wrap(SyncRequest.cmd, constructSyncRequest)
    val futureOpt = channelOpt match {
      case Some(channel) =>
        rpcClient.writeMessageWithChannel(channel, message)
      case None =>
        rpcClient.writeMessage(masterMachinePath, message)
    }

    futureOpt.foreach(addListenerToFuture)

    futureOpt
  }

  /**
   * process response from sync server
   */
  def processSyncData(response: InternalSyncProto.SyncResponse): Boolean = {
    val nosqlOpt = findNoSQLByResourceName(response.getResourceName)
    nosqlOpt match {
      case Some(nosql) =>
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
          val num = totalData.incrementAndGet()
          if ((num & MonadFaceConstants.NUM_OF_NEED_COMMIT) == 0) {
            info("{} row synchronized", num)
          }
        }
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
      resourceIndex += 1
      val r = resourceIndex < resources.length //未超出所有分区
      if (r)
        info("begin to sync resource:{}", resources(resourceIndex))

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

  private def finishRequest(): Unit = {
    try {
      afterDoing.set(true)
      afterFinishSync()
    } catch {
      case NonFatal(e) =>
        error(e.getMessage, e)
    } finally {
      afterDoing.set(false)
      semaphore.release()
    }
  }

  private def addListenerToFuture(future: ChannelFuture): Unit = {
    future.addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = {
        //cancel or fail
        if (!future.isSuccess)
          unlock(future.getChannel)
      }
    })
  }

  @tailrec
  private def constructSyncRequest: SyncRequest = {
    if (resourceIndex >= resources.length)
      throw new MonadException("reach resource range", MonadNodeExceptionCode.OVERFLOW_RESOURCE_RANGE)
    val rd = resources(resourceIndex)
    try {
      val builder = SyncRequest.newBuilder()
      builder.setPartitionId(getPartitionId)
      builder.setResourceName(rd)
      val nosqlOpt = findNoSQLByResourceName(rd)
      builder.setLogSeqFrom(nosqlOpt.get.FindLastBinlog() + 1)
      builder.setSize(ONE_BATCH_SIZE)
      info("start seq:{} batch_size:{}",builder.getLogSeqFrom,builder.getSize)
      builder.build()
    } catch {
      case e: Throwable =>
        error("[" + rd + "] fail to construct sync request ", e)
        resourceIndex += 1
        constructSyncRequest
    }
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
