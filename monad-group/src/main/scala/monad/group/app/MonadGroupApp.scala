// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.extjs.MonadExtjsConstants
import monad.face.MonadFaceConstants
import monad.group.MonadGroupModule
import stark.utils.services.{JettyServerSupport, SystemEnvDetectorSupport}
import org.slf4j.LoggerFactory

/**
 * monad管理平台
 * @author jcai
 * @version 0.1
 */
object MonadGroupApp
  extends JettyServerSupport
  with GlobalLoggerConfigurationSupport
  with SystemEnvDetectorSupport
  with BootstrapTextSupport {
  def main(args: Array[String]) {
    val serverHome = System.getProperty(MonadCoreSymbols.SERVER_HOME, "support")
    System.setProperty(MonadCoreSymbols.SERVER_HOME, serverHome)
    val config = MonadGroupModule.buildMonadGroupConfig(serverHome)
    configLogger(config.logFile, "GROUP", "monad", "ganshane")
    System.setProperty(MonadExtjsConstants.EXT_JS_DIR, config.extjsDir)

    val logger = LoggerFactory getLogger getClass
    logger.info("Starting group server ....")
    val classes = Array[Class[_]](
      Class.forName("monad.face.LocalMonadAssetModule"),
      Class.forName("monad.group.LocalMonadGroupModule"),
      Class.forName("monad.group.MonadGroupModule"),
      Class.forName("monad.extjs.MonadExtjsModule")
    )
    startServer(config.web, "monad.group", classes: _*)

    val version = readVersionNumber("META-INF/maven/com.ganshane.monad/monad-group/version.properties")
    printTextWithNative(logger, MonadFaceConstants.MONAD_TEXT_LOGO, "group@" + config.web.bind, version)
    logger.info("group server started")

    join()
  }
}

