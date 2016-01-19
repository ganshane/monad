// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id

import monad.core.MonadCoreSymbols
import monad.core.internal.MonadCoreUtils
import monad.id.config.MonadIdConfig
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.annotations.Symbol

/**
 * monad id module
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
object MonadIdModule {
  def buildMonadSyncConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val content = MonadCoreUtils.readConfigContent(serverHome,"monad-id.xml")
    XmlLoader.parseXML[MonadIdConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/id/monad-id.xsd")))
  }
}
