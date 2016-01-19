// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.face.MonadFaceConstants
import monad.id.MonadIdModule
import monad.jni.services.JniLoader
import monad.support.services.{SystemEnvDetectorSupport, TapestryIocContainerSupport}
import org.slf4j.LoggerFactory

/**
 * monad sync application
 */
object MonadIdApp
  extends TapestryIocContainerSupport
  with GlobalLoggerConfigurationSupport
  with SystemEnvDetectorSupport
  with BootstrapTextSupport {
  def main(args: Array[String]) {
    val serverHome = System.getProperty(MonadCoreSymbols.SERVER_HOME, "support")
    System.setProperty(MonadCoreSymbols.SERVER_HOME, serverHome)
    val config = MonadIdModule.buildMonadSyncConfig(serverHome)
    configLogger(config.logFile, "ID", "monad", "ganshane")
    //预先加载JNI文件
    JniLoader.loadJniLibrary(serverHome, config.logFile)

    val logger = LoggerFactory getLogger getClass
    logger.info("starting id server ....")
    val classes = List[Class[_]](
      Class.forName("monad.rpc.LocalRpcServerModule"),
      Class.forName("monad.rpc.LocalRpcModule"),
      Class.forName("monad.core.LocalMonadCoreModule"),
      Class.forName("monad.core.ThreadPoolModule"),
      Class.forName("monad.id.LocalMonadIdModule"),
      Class.forName("monad.id.MonadIdModule")
    )
    startUpContainer(classes: _*)

    val version = readVersionNumber("META-INF/maven/com.ganshane.monad/monad-id/version.properties")
    printTextWithNative(logger, MonadFaceConstants.MONAD_TEXT_LOGO, "id@" + config.rpc.bind, version)
    logger.info("id server started")

    join()
  }
}
