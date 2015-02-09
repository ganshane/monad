// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

/**
 * thread pool creator.
 * 因为jdk的ThreadPoolExecutor在队列未满的时候，不会扩充pool的大小
 */
object ThreadPoolCreator {

  def newSaturatingThreadPool(corePoolSize: Int,
                              maxPoolSize: Int,
                              maxQueueSize: Int,
                              keepAliveTime: Long,
                              timeUnit: TimeUnit,
                              threadNamePrefix: String,
                              rejectedExecutionHandler: RejectedExecutionHandler) = {
    val queue = new OverflowingSynchronousQueue[Runnable](maxQueueSize)
    val rejectionPolicyAdapter = new OverflowingRejectionPolicyAdapter(queue, rejectedExecutionHandler)
    val executor: ThreadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, queue, new NamedThreadFactory(threadNamePrefix), rejectionPolicyAdapter)
    executor
  }

  class NamedThreadFactory(prefix: String) extends ThreadFactory {
    private val seq = new AtomicInteger()

    def newThread(p1: Runnable) = {
      val thread = new Thread(p1)
      thread.setName("%s-%d".format(prefix, seq.incrementAndGet()))
      //设置优先级，让抽取和索引线程滞后处理
      thread.setPriority((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2)
      thread.setDaemon(true)
      thread
    }
  }

  class OverflowingRejectionPolicyAdapter(queue: OverflowingSynchronousQueue[Runnable],
                                          adaptedRejectedExecutionHandler: RejectedExecutionHandler)
    extends RejectedExecutionHandler {
    def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
      if (!queue.offerToOverflowingQueue(r)) {
        adaptedRejectedExecutionHandler.rejectedExecution(r, executor)
      }
    }
  }

  class OverflowingSynchronousQueue[E](capacity: Int) extends LinkedBlockingQueue[E](capacity) {
    private val synchronousQueue = new SynchronousQueue[E]()

    // Create a new thread or wake an idled thread
    override def offer(e: E) = synchronousQueue.offer(e)

    // Add to queue
    def offerToOverflowingQueue(e: E) = super.offer(e)

    override def take(): E = {
      // Return tasks from queue, if any, without blocking
      val task = super.poll()
      if (task != null) task else synchronousQueue.take()
    }

    override def poll(timeout: Long, unit: TimeUnit): E = {
      // Return tasks from queue, if any, without blocking
      val task = super.poll()
      if (task != null) task else synchronousQueue.poll(timeout, unit)
    }
  }

}
