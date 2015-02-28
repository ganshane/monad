// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.app

import monad.core.MonadCoreSymbols
import monad.core.services.{BootstrapTextSupport, GlobalLoggerConfigurationSupport}
import monad.extjs.MonadExtjsConstants
import monad.group.MonadGroupModule
import monad.support.services.{JettyServerSupport, SystemEnvDetectorSupport}
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
    configLogger(config.logFile, "GROUP")
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
    printTextWithNative("group@ " + config.web.bind,
      "META-INF/maven/com.ganshane.moand/monad-group/version.properties",
      0, logger)
    logger.info("group server started")

    join()
  }
}

/*
extends BaseJettyServer{
  //logger
  def main(args:Array[String]){
      val serverHome = System.getProperty(MonadSystemSymbols.MONAD_SERVER_HOME,"support")
      System.setProperty(MonadSystemSymbols.MONAD_SERVER_HOME,serverHome)
      val groupConfig = MonadGroupModule.buildMonadGroupConfig(serverHome)
      System.setProperty(MonadExtjsConstants.EXT_JS_DIR,groupConfig.extjsDir)

      ContainerUtil.configLogger(groupConfig.logFile,"GROUP")
      val logger = LoggerFactory getLogger  getClass
      logger.info("Starting group server ....")
      //config hazelcast using slf4j logger
      System.setProperty("hazelcast.logging.type","slf4j")
      val classes= Array[Class[_]](
          Class.forName("monad.core.LocalMonadAssetModule"),
          Class.forName("monad.core.LicenseModule"),
          Class.forName("monad.core.ObjectLifecycleModule"),
          Class.forName("monad.group.LocalMonadGroupModule"),
          Class.forName("monad.group.MonadGroupModule"),
          Class.forName("monad.extjs.MonadExtjsModule")
      )
      System.setProperty("tapestry.modules", classes.map(_.getName)mkString(","))
      val (server, contextHandler) = createServer("monad.core")
      server.start()
      val port=System.getProperty("server.port","9080").toInt
      ContainerUtil.printText("group@"+port,"META-INF/maven/com.egfit.monad/monad-all/version.properties")
      logger.info("Group server started")
      server.join()
  }
}
*/