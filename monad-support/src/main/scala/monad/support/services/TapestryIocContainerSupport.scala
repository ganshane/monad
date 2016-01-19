// Copyright 2011,2012,2013,2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.CountDownLatch

import org.apache.tapestry5.ioc.{Registry, RegistryBuilder}
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * tapestry container startup
 */
trait TapestryIocContainerSupport {
  val startSignal = new CountDownLatch(1)
  private var registry: Registry = null
  private var isClosed = false

  /**
   * startup tapestry
   */
  def startUpContainer(modules: Class[_]*) {
    val builder = new RegistryBuilder()
    builder.add(modules: _*)
    val m = System.getProperty("tapestry.modules")
    if (m != null)
      builder.add(m.split(',').map(Class.forName): _*)
    registry = builder.build()
    registry.performRegistryStartup()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        closeRegistry()
      }
    })
  }

  def closeRegistry() {
    synchronized {
      val logger = LoggerFactory getLogger getClass
      if (!isClosed) {
        try {
          logger.info("closing server ....")
          registry.shutdown()
          isClosed = true
          logger.info("server closed")
        } catch {
          case NonFatal(e) => logger.warn("fail to stop registry", e)
        }
      }
    }
  }

  def join() {
    startSignal.await()
  }

  def stopServer() {
    startSignal.countDown()
    closeRegistry()
    val logger = LoggerFactory getLogger getClass
    logger.info("Server  Terminated!")
  }

  /**
   * from container get service object
   */
  def getService[T](clazz: Class[T]): T = registry.getService(clazz)

  def getService[T](clazz: Class[T], serviceId: String): T = registry.getService(serviceId, clazz)
}
