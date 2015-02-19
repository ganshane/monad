// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync

import monad.core.services.ServiceLifecycleHub
import monad.face.MonadFaceConstants
import monad.face.services.ResourceDefinitionLoaderListener
import monad.support.services.ServiceLifecycle
import monad.sync.internal.{ResourceImporterManager, ResourceSyncServer}
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.ioc.{OrderedConfiguration, ServiceBinder}

/**
 * 本地的同步模块
 * @author jcai
 */
object LocalMonadSyncModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[ResourceImporterManager]).withId("ResourceImporterManager")
    binder.bind(classOf[ResourceSyncServer]).withId("ResourceSyncServer")
  }

  @Contribute(classOf[ResourceDefinitionLoaderListener])
  def provideResourceDefinitionLoaderListener(
                                               configuration: OrderedConfiguration[ResourceDefinitionLoaderListener],
                                               resourceImporterManager: ResourceImporterManager) {
    configuration.add("importer", resourceImporterManager, "after:node")
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideServiceLifecycle(configuration: OrderedConfiguration[ServiceLifecycle],
                              importerManager: ResourceImporterManager) {
    configuration.add(MonadFaceConstants.LIFE_IMPORTER, importerManager,
      "after:" + MonadFaceConstants.LIFE_INDEXER,
      "after:" + MonadFaceConstants.LIFE_GROUP_ZOOKEEPER
    )
  }
}
