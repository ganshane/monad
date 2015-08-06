// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.face.MonadFaceConstants
import monad.jni.services.JniLoader
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
    configLogger(config.logFile, "SYNC", "monad", "ganshane")
    //预先加载JNI文件
    JniLoader.loadJniLibrary(serverHome, config.logFile)

    val logger = LoggerFactory getLogger getClass
    logger.info("Starting sync server ....")
    val classes = List[Class[_]](
      Class.forName("monad.rpc.LocalRpcServerModule"),
      Class.forName("monad.rpc.LocalRpcClientModule"),
      Class.forName("monad.core.LocalMonadCoreModule"),
      Class.forName("monad.face.RemoteGroupModule"),
      Class.forName("monad.core.ThreadPoolModule"),
      Class.forName("monad.sync.LocalMonadSyncModule"),
      Class.forName("monad.sync.MonadSyncModule"),
      Class.forName("monad.rpc.LocalRpcModule"),
      Class.forName("monad.face.ResourceModule")
    )
    startUpContainer(classes: _*)

    val version = readVersionNumber("META-INF/maven/com.ganshane.monad/monad-sync/version.properties")
    printTextWithNative(logger, MonadFaceConstants.MONAD_TEXT_LOGO, "sync@" + config.rpc.bind, version)
    logger.info("sync server started")

    join()
  }
}
