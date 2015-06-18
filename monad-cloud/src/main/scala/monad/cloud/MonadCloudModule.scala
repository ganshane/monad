// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud

import monad.cloud.config.MonadCloudConfig
import monad.core.MonadCoreSymbols
import monad.core.internal.MonadConfigFileUtils
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.annotations.Symbol

/**
 * monad cloud module
 * cloud中保存的目录结构如下：
 *
 * /-monad 根目录
 * |-machines 所有机器信息
 * |-node-001
 * |-sync
 **/
object MonadCloudModule {
  def buildMonadCloudConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val content = MonadConfigFileUtils.readConfigFileContent(serverHome, "monad-cloud.xml")
    XmlLoader.parseXML[MonadCloudConfig](content,xsd = Some(getClass.getResourceAsStream("/monad/cloud/monad-cloud.xsd")))

    //MonadCoreUtils.autoFixIpAddress(config)

  }
}
