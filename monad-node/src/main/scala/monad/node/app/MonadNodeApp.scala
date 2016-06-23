// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.face.MonadFaceConstants
import monad.jni.services.JniLoader
import monad.node.MonadNodeModule
import stark.utils.services.{SystemEnvDetectorSupport, TapestryIocContainerSupport}
import org.slf4j.LoggerFactory

/**
 * processor application
 */
object MonadNodeApp
  extends TapestryIocContainerSupport
  with GlobalLoggerConfigurationSupport
  with SystemEnvDetectorSupport
  with BootstrapTextSupport {
  def main(args: Array[String]) {
    val serverHome = System.getProperty(MonadCoreSymbols.SERVER_HOME, "support")
    System.setProperty(MonadCoreSymbols.SERVER_HOME, serverHome)
    val config = MonadNodeModule.buildMonadNodeConfig(serverHome)
    configLogger(config.logFile, "NODE", "monad", "ganshane")
    //预先加载JNI文件
    JniLoader.loadJniLibrary(serverHome, config.logFile)

    val logger = LoggerFactory getLogger getClass
    logger.info("Starting node server ....")
    val classes = List[Class[_]](
      Class.forName("monad.core.LocalMonadCoreModule"),
      Class.forName("monad.face.ResourceModule"),
      Class.forName("monad.core.ThreadPoolModule"),
      Class.forName("monad.face.RemoteGroupModule"),
      Class.forName("stark.rpc.LocalRpcModule"),
      Class.forName("stark.rpc.LocalRpcClientModule"),
      Class.forName("stark.rpc.LocalRpcServerModule"),
      Class.forName("monad.node.LocalMonadNodeModule"),
      Class.forName("monad.node.MonadNodeModule")
    )
    startUpContainer(classes: _*)

    val version = readVersionNumber("META-INF/maven/com.ganshane.monad/monad-node/version.properties")
    printTextWithNative(logger, MonadFaceConstants.MONAD_TEXT_LOGO, "node@" + config.rpc.bind, version)
    logger.info("monad server started")

    join()
  }
}
