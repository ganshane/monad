// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.support.services.{SystemEnvDetectorSupport, TapestryIocContainerSupport}
import monad.sync.MonadSyncModule
import org.slf4j.LoggerFactory

/**
 * monad sync application
 */
object MonadSyncApp
  extends TapestryIocContainerSupport
  with GlobalLoggerConfigurationSupport
  with SystemEnvDetectorSupport
  with BootstrapTextSupport {
  def main(args: Array[String]) {
    val serverHome = System.getProperty(MonadCoreSymbols.SERVER_HOME, "support")
    System.setProperty(MonadCoreSymbols.SERVER_HOME, serverHome)
    val config = MonadSyncModule.buildMonadSyncConfig(serverHome)
    configLogger(config.logFile, "SYNC")

    val logger = LoggerFactory getLogger getClass
    logger.info("Starting sync server ....")
    val classes = List[Class[_]](
      Class.forName("monad.core.LocalMonadCoreModule"),
      Class.forName("monad.core.ServiceLifecycleModule"),
      Class.forName("monad.face.ResourceModule"),
      Class.forName("monad.face.RemoteGroupModule"),
      Class.forName("monad.rpc.LocalRpcModule"),
      Class.forName("monad.rpc.LocalRpcServerModule"),
      Class.forName("monad.sync.LocalMonadSyncModule"),
      Class.forName("monad.sync.MonadSyncModule")
    )
    startUpContainer(classes: _*)
    printTextWithNative("sync@" + config.rpc.bind,
      "META-INF/maven/com.ganshane.monad/monad-sync/version.properties",
      0, logger)
    logger.info("sync server started")

    join()
  }
}
