// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import monad.core.MonadCoreSymbols
import monad.core.config.LogFileSupport
import monad.core.services.ServiceLifecycleHub
import monad.jni.services.JniLoader
import monad.support.services.{LoggerSupport, ServiceLifecycle}
import org.apache.tapestry5.ioc.annotations.Symbol

import scala.collection.JavaConversions._


/**
 * service lifecycle hub
 */
class ServiceLifecycleHubImpl(configuration: java.util.List[ServiceLifecycle],
                              @Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String,
                              logFileConfig: LogFileSupport)
  extends ServiceLifecycleHub
  with LoggerSupport {
  //FIXES #47 构建hub的时候立即启动，如果出现失败，则应用停止启动
  start()

  /**
   * 服务关闭
   */
  override def shutdown(): Unit = {
    configuration.reverse.foreach({
      x =>
        try {
          x.shutdown()
        } catch {
          case e: Throwable =>
            error(e.getMessage)
        }
    })
  }

  /**
   * 启动服务
   */
  override def start(): Unit = {
    JniLoader.loadJniLibrary(serverHome, logFileConfig.logFile)
    configuration.foreach(_.start())
  }
}
