// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.app

import monad.api.MonadApiModule
import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.face.MonadFaceConstants
import monad.support.services.{JettyServerSupport, SystemEnvDetectorSupport}
import org.slf4j.LoggerFactory

/**
 * processor application
 */
object MonadApiApp
  extends JettyServerSupport
  with GlobalLoggerConfigurationSupport
  with SystemEnvDetectorSupport
  with BootstrapTextSupport {
  def main(args: Array[String]) {
    val serverHome = System.getProperty(MonadCoreSymbols.SERVER_HOME, "support")
    System.setProperty(MonadCoreSymbols.SERVER_HOME, serverHome)
    val config = MonadApiModule.buildMonadApiConfig(serverHome)
    configLogger(config.logFile, "API", "monad", "ganshane")

    val logger = LoggerFactory getLogger getClass
    logger.info("Starting api server ....")
    val classes = List[Class[_]](
      Class.forName("monad.core.LocalMonadCoreModule"),
      Class.forName("stark.rpc.LocalRpcModule"),
      Class.forName("stark.rpc.LocalRpcClientModule"),
      //Class.forName("monad.core.ProtobufProcessorModule"),
      Class.forName("monad.face.LocalMonadAssetModule"),
      Class.forName("monad.face.ResourceModule"),
      Class.forName("monad.core.ThreadPoolModule"),
      Class.forName("monad.face.RemoteGroupModule"),
      Class.forName("monad.api.LocalMonadApiModule"),
      Class.forName("monad.api.MonadApiModule")
    )
    startServer(config.web, "monad.api", classes: _*)

    val version = readVersionNumber("META-INF/maven/com.ganshane.monad/monad-api/version.properties")
    printTextWithNative(logger, MonadFaceConstants.MONAD_TEXT_LOGO, "api@" + config.web.bind, version)

    logger.info("api server started")

    join()
  }
}
