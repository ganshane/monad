// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.jni.services.JniLoader
import monad.node.MonadNodeModule
import monad.support.services.{SystemEnvDetectorSupport, TapestryIocContainerSupport}
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
      Class.forName("monad.rpc.LocalRpcModule"),
      Class.forName("monad.rpc.LocalRpcClientModule"),
      Class.forName("monad.rpc.LocalRpcServerModule"),
      Class.forName("monad.node.LocalMonadNodeModule"),
      Class.forName("monad.node.MonadNodeModule")
    )
    startUpContainer(classes: _*)
    printTextWithNative("node",
      "META-INF/maven/com.ganshane.monad/monad-node/version.properties",
      0, logger)
    logger.info("monad server started")

    join()
  }
}
