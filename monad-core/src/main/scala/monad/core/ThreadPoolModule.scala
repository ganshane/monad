// Copyright 2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

import monad.support.services.MonadUtils
import org.apache.tapestry5.ioc.annotations._
import org.apache.tapestry5.ioc.internal.services.ParallelExecutorImpl
import org.apache.tapestry5.ioc.internal.services.cron.PeriodicExecutorImpl
import org.apache.tapestry5.ioc.services._
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor
import org.apache.tapestry5.ioc.util.TimeInterval
import org.apache.tapestry5.ioc.{IOCSymbols, MappedConfiguration}
import org.slf4j.{Logger, LoggerFactory}

/**
 * thread pool module
 * @author jcai
 */
object ThreadPoolModule {
  @Contribute(classOf[ServiceOverride])
  def provideParallelExecutor(configuration: MappedConfiguration[Class[_], Object],
                              @Local periodicExecutor: PeriodicExecutor,
                              @Local parallelExecutor: ParallelExecutor) {
    configuration.add(classOf[PeriodicExecutor], periodicExecutor)
    configuration.add(classOf[ParallelExecutor], parallelExecutor)
  }

  @ServiceId("MonadPeriodicExecutor")
  def buildPeriodicExecutor(@Local parallelExecutor: ParallelExecutor, logger: Logger) = {
    new PeriodicExecutorImpl(parallelExecutor, logger)

  }

  def buildParallelExecutor(@Local executorService: ExecutorService,
                            thunkCreator: ThunkCreator, threadManager: PerthreadManager): ParallelExecutor = {
    new ParallelExecutorImpl(executorService, thunkCreator, threadManager) /* new PerthreadManager {
      def cleanup() {}

      def run(runnable: Runnable) {
        throw new UnsupportedOperationException
      }

      def invoke[T](invokable: Invokable[T]) = {
        throw new UnsupportedOperationException
      }

      def addThreadCleanupListener(listener: ThreadCleanupListener) {
        throw new UnsupportedOperationException
      }

      def createValue[T]() = {
        throw new UnsupportedOperationException
      }
    })
    */
  }

  def buildExecutorService(@Symbol(IOCSymbols.THREAD_POOL_CORE_SIZE)
                           coreSize: Int,
                           @Symbol(IOCSymbols.THREAD_POOL_MAX_SIZE)
                           maxSize: Int,
                           @Symbol(IOCSymbols.THREAD_POOL_KEEP_ALIVE)
                           @IntermediateType(classOf[TimeInterval])
                           keepAliveMillis: Int,
                           @Symbol(IOCSymbols.THREAD_POOL_QUEUE_SIZE)
                           queueSize: Int,
                           perthreadManager: PerthreadManager,
                           shutdownHub: RegistryShutdownHub
                            ): ExecutorService = {

    val threadFactory = new ThreadFactory {
      private val seq = new AtomicInteger()

      def newThread(p1: Runnable) = {
        val thread = new Thread(p1)
        thread.setName("monad-background-%d".format(seq.incrementAndGet()))
        //设置优先级，让抽取和索引线程滞后处理
        thread.setPriority((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2)
        thread.setDaemon(true)
        thread
      }
    }
    //val executorService = Executors.newFixedThreadPool(coreSize,threadFactory)
    val workQueue = new OverflowingSynchronousQueue[Runnable](queueSize)
    val logger = LoggerFactory getLogger getClass
    val executorService = new ThreadPoolExecutor(coreSize, maxSize, keepAliveMillis,
      TimeUnit.MILLISECONDS, workQueue, threadFactory, new RejectedExecutionHandler {
        def rejectedExecution(p1: Runnable, p2: ThreadPoolExecutor) {
          logger.debug("try to offer to overflowingQueue")
          if (!workQueue.offerToOverflowingQueue(p1)) {
            logger.error("no more thread,so reject")
          }
        }
      })


    shutdownHub.addRegistryShutdownListener(new Runnable() {
      def run() {
        MonadUtils.shutdownExecutor(executorService, "global executor")
      }
    })

    executorService
  }

  @Contribute(classOf[SymbolProvider])
  @ApplicationDefaults
  def provideApplicationDefaults(configuration: MappedConfiguration[String, Any]) {
    configuration.add(IOCSymbols.THREAD_POOL_CORE_SIZE, 2)
    configuration.add(IOCSymbols.THREAD_POOL_MAX_SIZE, 20)
    configuration.add(IOCSymbols.THREAD_POOL_KEEP_ALIVE, "1 m")
    configuration.add(IOCSymbols.THREAD_POOL_ENABLED, true)
    configuration.add(IOCSymbols.THREAD_POOL_QUEUE_SIZE, 200)
  }

  class DelegateQueue(size: Int) extends LinkedBlockingQueue[Runnable](size) {
    override def offer(p1: Runnable) = false

    def offerInFact(p1: Runnable) = {
      super.offer(p1)
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
