// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api

import monad.api.config.MonadApiConfig
import monad.core.MonadCoreSymbols
import monad.core.internal.MonadCoreUtils
import stark.utils.services.XmlLoader
import org.apache.tapestry5.ioc.annotations._

/**
 * monad api module
 * @author jcai
 */
object MonadApiModule {
  def buildMonadApiConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val content = MonadCoreUtils.readConfigContent(serverHome, "monad-api.xml")
    XmlLoader.parseXML[MonadApiConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/api/monad-api.xsd")))
  }
}
