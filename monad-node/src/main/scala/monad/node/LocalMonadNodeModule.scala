// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node

import monad.core.services.ServiceLifecycleHub
import monad.face.MonadFaceConstants
import monad.face.services._
import monad.node.internal._
import monad.node.services.ResourceIndexerManager
import monad.support.services.ServiceLifecycle
import org.apache.tapestry5.ioc.annotations._
import org.apache.tapestry5.ioc.services.Builtin
import org.apache.tapestry5.ioc.{MappedConfiguration, OrderedConfiguration, ServiceBinder}

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
}
