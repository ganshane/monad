// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import monad.rpc.protocol.CommandProto.BaseCommand
import monad.rpc.services.RpcClientMerger
import org.jboss.netty.channel.{Channel, ChannelFuture, ChannelFutureListener}

/**
 * 针对多个服务器进行请求处理
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-26
 */
object AsyncTaskMonitor {
  private val tasks = new ConcurrentHashMap[Long, InternalRequestMerger[_]]()
  private val taskIdSeq = new AtomicLong(0)

  /**
   * 发现任务
   */
  def findTask(taskId: Long) = tasks.get(taskId)

  /**
   * 创建一个阻塞任务的Future对象
   */
  def createBlockTask() = {
    createMergerTask(1, new RpcClientMerger[BaseCommand] {
      private var command: BaseCommand = _

      /**
       * 处理接受的消息
       */
      override def handle(commandRequest: BaseCommand, channel: Channel): Unit = {
        command = commandRequest
      }

      /**
       * 得到merge之后的结果
       * @return merge之后的结果
       */
      override def get: BaseCommand = {
        command
      }
    })
  }

  def createMergerTask[T](serverSize: Int, rpcMerger: RpcClientMerger[T]) = {
    val taskId = produceTaskId
    val future = new InternalRequestMerger[T](taskId, rpcMerger)
    tasks.put(taskId, future)
    future.startRequest(taskId, serverSize)

    future
  }

  /**
   * 产生一个任务ID
   */
  def produceTaskId = taskIdSeq.incrementAndGet()

  private[internal] class InternalRequestMerger[T](val taskId: Long, rpcMerger: RpcClientMerger[T]) extends Future[T] {

    private var cancelled = false
    private var countDownLatch: CountDownLatch = _

    private val channelCloseListener =  new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = {
        countDown()
      }
    }

    /**
     * 对获得的消息进行处理
     */
    def handle(commandRequest: BaseCommand, channel: Channel): Unit = {
      channel.getCloseFuture.removeListener(channelCloseListener)
      rpcMerger.handle(commandRequest, channel)
    }

    def monitorChannel(f: ChannelFuture) = {
      //当在客户端写入失败，则应该减少计数器
      f.addListener(new ChannelFutureListener {
        override def operationComplete(channelFuture: ChannelFuture): Unit = {
          if (!channelFuture.isSuccess) {
            countDown()
          }
        }
      })

      f.getChannel.getCloseFuture.addListener(channelCloseListener)
    }
    override def get(): T = {
      countDownLatch.await()
      rpcMerger.get
    }

    /**
     * 开始请求
     * @param taskId 任务ID号
     * @param serverSize 服务器的个数
     */
    def startRequest(taskId: Long, serverSize: Int): Unit = {
      countDownLatch = new CountDownLatch(serverSize)
      tasks.put(taskId, this)
    }


    override def cancel(mayInterruptIfRunning: Boolean): Boolean = {
      cancelled = true

      cancelled
    }

    override def isCancelled: Boolean = {
      cancelled
    }


    override def get(timeout: Long, unit: TimeUnit): T = {
      //TODO 增加超时设置
      if (countDownLatch.await(timeout, unit)) {
        rpcMerger.get
      } else {
        throw new TimeoutException("rpc timeout " + unit.toMillis(timeout) + " mills")
      }
    }

    def countDown(): Unit = {
      countDownLatch.countDown()
      if (isDone) //完成的话，则删除此任务
        tasks.remove(taskId)
    }

    override def isDone: Boolean = {
      countDownLatch.getCount == 0
    }
  }

}


