// Copyright 2012,2013,2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud.app

import monad.cloud.MonadCloudModule
import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.support.services.{SystemEnvDetectorSupport, TapestryIocContainerSupport}
import org.slf4j.LoggerFactory

/**
 * 集群服务器
 * @author jcai
 */
object MonadCloudApp
  extends TapestryIocContainerSupport
  with GlobalLoggerConfigurationSupport
  with SystemEnvDetectorSupport
  with BootstrapTextSupport {
  def main(args: Array[String]) {
    val serverHome = System.getProperty(MonadCoreSymbols.SERVER_HOME, "support")
    System.setProperty(MonadCoreSymbols.SERVER_HOME, serverHome)
    val config = MonadCloudModule.buildMonadCloudConfig(serverHome)
    configLogger(config.logFile, "CLOUD", "monad", "ganshane")

    val logger = LoggerFactory getLogger getClass
    logger.info("Starting cloud server ....")
    val classes = Array[Class[_]](
      Class.forName("monad.cloud.LocalCloudModule"),
      Class.forName("monad.cloud.MonadCloudModule")
    )
    startUpContainer(classes: _*)
    val port = config.port

    printTextWithNative("cloud@" + port,
      "META-INF/maven/com.ganshane.monad/monad-cloud/version.properties",
      0, logger)
    logger.info("Cluster server started ")
    join()
  }
}
