// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api

import monad.api.config.MonadApiConfig
import monad.api.internal.{CollectMaxDocResultMerger, CollectSearchResultMerger, FindIdSeqMerger, IdSearchMerger}
import monad.core.MonadCoreSymbols
import monad.face.services._
import monad.rpc.services.{RpcRemoteServiceCreator, RpcResultMergeSource, RpcResultMerger}
import monad.support.MonadSupportConstants
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.MappedConfiguration
import org.apache.tapestry5.ioc.annotations._
import org.apache.tapestry5.ioc.services.Builtin

import scala.io.Source

/**
 * monad api module
 * @author jcai
 */
object MonadApiModule {
  @Marker(Array(classOf[Builtin]))
  def buildResourceSearcherSupport(rpcCreator: RpcRemoteServiceCreator) = {
    rpcCreator.createRemoteInstance(classOf[RpcSearcherFacade])
  }

  def buildIdFacade(rpcCreator: RpcRemoteServiceCreator) = {
    rpcCreator.createRemoteInstance(classOf[IdFacade])
  }

  @Contribute(classOf[RpcResultMergeSource])
  def provideCollectSearchMerger(configuration: MappedConfiguration[String, Class[_ <: RpcResultMerger[_]]]) {
    configuration.add("collectSearch", classOf[CollectSearchResultMerger])
    configuration.add("collectMaxDoc", classOf[CollectMaxDocResultMerger])
    configuration.add("IdSearchMerger", classOf[IdSearchMerger])
    configuration.add("FindIdSeqMerger", classOf[FindIdSeqMerger])
  }

  def buildMonadApiConfig(@Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) = {
    val filePath = serverHome + "/config/monad-api.xml"
    val content = Source.fromFile(filePath, MonadSupportConstants.UTF8_ENCODING).mkString
    XmlLoader.parseXML[MonadApiConfig](content, xsd = Some(getClass.getResourceAsStream("/monad/api/monad-api.xsd")))
  }
}
