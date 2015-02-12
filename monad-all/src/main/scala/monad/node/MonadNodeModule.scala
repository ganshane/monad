// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node

import monad.core.MonadCoreSymbols
import monad.face.services._
import monad.node.config.MonadNodeConfig
import monad.rpc.services.RpcRegistry
import monad.support.MonadSupportConstants
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.Configuration
import org.apache.tapestry5.ioc.annotations.{Contribute, Symbol}

import scala.io.Source

/**
 * monad node module
 * @author jcai
 */
object MonadNodeModule {
  @Contribute(classOf[RpcRegistry])
  def provideRpcRegistry(config: Configuration[String]) {
    config.add(classOf[RpcSearcherFacade].getName)
  }

  def buildMonadNodeConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val filePath = serverHome + "/config/monad-node.xml"
    val content = Source.fromFile(filePath, MonadSupportConstants.UTF8_ENCODING).mkString
    val config = XmlLoader.parseXML[MonadNodeConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/node/monad-node.xsd")))
    System.setProperty("server.id", config.regionId.toString)

    config
  }
}
