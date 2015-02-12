// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node

import monad.core.services.{ResourceDefinitionLoaderListener, ResourceRequest, ServiceLifecycleHub}
import monad.face.MonadFaceConstants
import monad.face.services._
import monad.node.internal._
import monad.node.services.{MonadNodeExceptionCode, ResourceIndexer, ResourceIndexerManager}
import monad.support.services.{MonadException, ServiceLifecycle}
import org.apache.tapestry5.ioc.annotations._
import org.apache.tapestry5.ioc.services.Builtin
import org.apache.tapestry5.ioc.{MappedConfiguration, OrderedConfiguration, ScopeConstants, ServiceBinder}

/**
 * 本地的节点模块
 * @author jcai
 */
object LocalMonadNodeModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[ResourceIndexerManager], classOf[ResourceIndexerManagerImpl]).
      withId("ResourceIndexerManager").
      withMarker(classOf[Builtin])
    binder.bind(classOf[ResourceSearcherSource], classOf[ResourceSearcherSourceImpl]).
      withId("ResourceSearcherSource")
    binder.bind(classOf[DocumentSource], classOf[DocumentSourceImpl]).withId("DocumentSource")
  }

  @Contribute(classOf[ResourceSearcherSource])
  def provideDefaultResourceSearcherPluginFactory(configuration: MappedConfiguration[String, ResourceSearcherFactory]) {
    configuration.addInstance(MonadFaceConstants.DEFAULT_RESOURCE_SEARCHER_FACTORY, classOf[DefaultResourceSearcherFactory])
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideServiceLifecycle(configuration: OrderedConfiguration[ServiceLifecycle],
                              indexerManager: ResourceIndexerManager) {
    configuration.add(MonadFaceConstants.LIFE_INDEXER, indexerManager,
      "after:" + MonadFaceConstants.LIFE_GROUP_ZOOKEEPER)
  }

  @Contribute(classOf[ResourceDefinitionLoaderListener])
  def provideResourceDefinitionLoaderListener(
                                               configuration: OrderedConfiguration[ResourceDefinitionLoaderListener],
                                               resourceIndexerManager: ResourceIndexerManager) {
    configuration.add("node", resourceIndexerManager, "before:*")
  }

  @Marker(Array(classOf[Builtin]))
  @Scope(ScopeConstants.PERTHREAD)
  def buildResourceSearcher(resourceRequest: ResourceRequest, indexerManager: ResourceIndexerManager): ResourceSearcher = {
    val name = resourceRequest.getResourceDefinition.name
    val indexer = indexerManager.getObject(name)
    if (indexer == null) {
      throw new MonadException("未能获取搜索类" + name,
        MonadNodeExceptionCode.RESOURCE_SEARCH_NO_FOUND
      )
    }
    indexer.getResourceSearcher
  }

  @Scope(ScopeConstants.PERTHREAD)
  def buildResourceIndexer(resourceRequest: ResourceRequest, indexerManager: ResourceIndexerManager): ResourceIndexer = {
    indexerManager.getObject(resourceRequest.getResourceDefinition.name)
  }
}
