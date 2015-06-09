// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node

import monad.core.MonadCoreSymbols
import monad.core.internal.MonadCoreUtils
import monad.node.config.MonadNodeConfig
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.annotations.Symbol

/**
 * monad node module
 * @author jcai
 */
object MonadNodeModule {
  def buildMonadNodeConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val content = MonadCoreUtils.readConfigContent(serverHome,"monad-node.xml")
    XmlLoader.parseXML[MonadNodeConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/node/monad-node.xsd")))
  }
}
