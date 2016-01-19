// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group

import monad.core.MonadCoreSymbols
import monad.core.internal.MonadCoreUtils
import monad.group.config.MonadGroupConfig
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.annotations.Symbol

/**
 * monad group module
 * @author jcai
 */
object MonadGroupModule {
  def buildMonadGroupConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val content = MonadCoreUtils.readConfigContent(serverHome,"monad-group.xml")
    XmlLoader.parseXML[MonadGroupConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/group/monad-group.xsd")))
  }
}
