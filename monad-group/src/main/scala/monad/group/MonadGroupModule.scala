// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group

import monad.core.MonadCoreSymbols
import monad.group.config.MonadGroupConfig
import monad.support.MonadSupportConstants
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.annotations.Symbol

import scala.io.Source

/**
 * monad group module
 * @author jcai
 */
object MonadGroupModule {
  def buildMonadGroupConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val filePath = serverHome + "/config/monad-group.xml"
    val content = Source.fromFile(filePath, MonadSupportConstants.UTF8_ENCODING).mkString
    XmlLoader.parseXML[MonadGroupConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/group/monad-group.xsd")))
  }
}
