// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync

import monad.core.MonadCoreSymbols
import monad.support.MonadSupportConstants
import monad.support.services.XmlLoader
import monad.sync.config.MonadSyncConfig
import org.apache.tapestry5.ioc.annotations.Symbol

import scala.io.Source

/**
 * monad synchronizer module
 * @author jcai
 * @version 0.1
 */
object MonadSyncModule {
  /*
  def buildIdFacade(rpcCreator:RpcRemoteServiceCreator)={
      rpcCreator.createRemoteInstance(classOf[IdFacade])
  }
  */
  def buildMonadSyncConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val filePath = serverHome + "/config/monad-sync.xml"
    val content = Source.fromFile(filePath, MonadSupportConstants.UTF8_ENCODING).mkString
    XmlLoader.parseXML[MonadSyncConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/sync/monad-sync.xsd")))
  }
}
