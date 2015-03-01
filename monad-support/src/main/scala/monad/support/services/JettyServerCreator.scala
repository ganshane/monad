// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.net.BindException
import java.util
import java.util.concurrent._
import javax.servlet.{DispatcherType, ServletContext}

import org.apache.tapestry5.TapestryFilter
import org.apache.tapestry5.internal.InternalConstants
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.{Connector, Server}
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler, ServletHolder}
import org.eclipse.jetty.util.thread.QueuedThreadPool

trait JettyServerSupport {
  private var modules: Option[Array[Class[_]]] = None
  private var serverOpt: Option[Server] = None

  protected def startServer(config: WebServerConfig, pkg: String, classes: Class[_]*): Server = {
    this.modules = Some(classes.toArray)
    try {
      val bind = MonadUtils.parseBind(config.bind)
      val server = JettyServerCreator.createTapestryWebapp(bind._1, bind._2, pkg, "monad", new MonadTapestryFilter)
      JettyServerCreator.configServer(server, config)
      server.start()
      serverOpt = Some(server)
      server
    } catch {
      case e: BindException =>
        throw new MonadException(e.getMessage, MonadSupportErrorCode.SERVER_FAIL_BIND)
    }
  }

  protected def join() {
    serverOpt.foreach(_.join())
  }

  protected def shutdownServer() {
    serverOpt.foreach(_.stop())
  }

  class MonadTapestryFilter extends TapestryFilter {
    override def provideExtraModuleClasses(context: ServletContext): Array[Class[_]] = {
      modules match {
        case Some(m) =>
          m
        case None =>
          super.provideExtraModuleClasses(context)
      }
    }
  }

}

/**
 * jetty servelt container
 */
object JettyServerCreator {
  /**
   * 创建tapestry的webapp程序
   * @param port 端口
   * @param pkg tapestry package
   * @param appName 服务器实例
   * @param filter filter全路径
   * @return server instance
   */
  def createTapestryWebapp(host: String,
                           port: Int,
                           pkg: String,
                           appName: String = "tapestry",
                           filter: TapestryFilter = new TapestryFilter): Server = {
    //TODO 提前校验端口是否被占用
    val server = new Server()
    //对connector进行配置
    val connector = new SelectChannelConnector
    connector.setHost(host)
    connector.setPort(port)


    server.setConnectors(Array[Connector](connector))


    server.setSendServerVersion(false)
    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath("/")
    context.setDisplayName(appName)
    context.setInitParameter(InternalConstants.TAPESTRY_APP_PACKAGE_PARAM, pkg)
    //default servlet holder
    val servletHolder = new ServletHolder(classOf[DefaultServlet])
    servletHolder.setName("default")
    context.addServlet(servletHolder, "/")

    //tapestry filter
    val filterHolder = new FilterHolder(filter)
    filterHolder.setName(appName)


    val all = util.EnumSet.allOf(classOf[DispatcherType])
    context.addFilter(filterHolder, "/*", all)
    server.setHandler(context)
    server
  }

  def configServer(server: Server, webServerConfig: WebServerConfig) {
    /*
    val executorService = ThreadPoolCreator.newSaturatingThreadPool(
      webServerConfig.minConnCount,
      webServerConfig.maxConnCount,
      webServerConfig.waitingQueueSize,
      webServerConfig.keepAliveTimeInMinutes,
      TimeUnit.MINUTES,
      "monad-web",
      new RejectedExecutionHandler {
        override def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor): Unit = {
          throw new RejectedExecutionException("reach max connection")
        }
      })
    val threadPool = new ExecutorThreadPool(executorService)
    */
    val threadPool = new QueuedThreadPool(new ArrayBlockingQueue[Runnable](6000))
    threadPool.setMinThreads(webServerConfig.minConnCount)
    threadPool.setMaxThreads(webServerConfig.maxConnCount)
    threadPool.setMaxIdleTimeMs(TimeUnit.MINUTES.toMillis(webServerConfig.keepAliveTimeInMinutes).asInstanceOf[Int])
    threadPool.setDaemon(true)

    server.setThreadPool(threadPool)

    val connector = server.getConnectors.head.asInstanceOf[SelectChannelConnector]
    //connector.setAcceptors(Math.max(1, (Runtime.getRuntime.availableProcessors + 3) / 4))

    if (webServerConfig.acceptor == 0)
      connector.setAcceptors(Math.min(4, Runtime.getRuntime.availableProcessors + 3) / 4)
    else
      connector.setAcceptors(webServerConfig.acceptor)
    connector.setAcceptQueueSize(webServerConfig.backlog)
    connector.setMaxIdleTime(webServerConfig.idleTimeSecs * 1000)
    connector.setRequestBufferSize(webServerConfig.requestBufferSizeKB * 1024)
    connector.setResponseBufferSize(webServerConfig.responseBufferSizeKB * 1024)


    //graceful shutdown
    server.setStopAtShutdown(true)
    server.setGracefulShutdown(5000)
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
