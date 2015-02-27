// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api

import monad.api.config.MonadApiConfig
import monad.core.MonadCoreSymbols
import monad.support.MonadSupportConstants
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.annotations._

import scala.io.Source

/**
 * monad api module
 * @author jcai
 */
object MonadApiModule {
  def buildMonadApiConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val filePath = serverHome + "/config/monad-api.xml"
    val content = Source.fromFile(filePath, MonadSupportConstants.UTF8_ENCODING).mkString
    XmlLoader.parseXML[MonadApiConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/api/monad-api.xsd")))
  }
}
