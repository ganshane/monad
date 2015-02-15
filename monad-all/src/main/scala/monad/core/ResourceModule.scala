// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core

import monad.api.internal.ResourceRequestImpl
import monad.api.services.ResourceRequest
import monad.core.internal.ResourcesWatcher
import monad.core.services.{ResourceDefinitionLoaderListener, ServiceLifecycleHub}
import monad.face.MonadFaceConstants
import monad.face.services.ResourceDefinitionLoader
import monad.support.services.ServiceLifecycle
import org.apache.tapestry5.ioc.annotations.{Contribute, Local, Marker}
import org.apache.tapestry5.ioc.services.ChainBuilder
import org.apache.tapestry5.ioc.{OrderedConfiguration, ScopeConstants, ServiceBinder}
import org.apache.tapestry5.services.Core

/**
 * 提供资源模块
 * @author jcai
 */
object ResourceModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[ResourceRequest], classOf[ResourceRequestImpl]).
      scope(ScopeConstants.PERTHREAD).
      withId("ResourceRequest")
    binder.bind(classOf[ResourceDefinitionLoader], classOf[ResourcesWatcher]).withId("ResourceDefinitionLoader")
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideServiceLifecycle(configuration: OrderedConfiguration[ServiceLifecycle],
                              @Local resourceLoader: ResourceDefinitionLoader) {
    configuration.add(MonadFaceConstants.LIFE_RESOURCES, resourceLoader, "after:*")
  }

  @Marker(Array(classOf[Core]))
  def buildResourceDefinitionLoaderListener(
                                             configuration: java.util.List[ResourceDefinitionLoaderListener],
                                             chainBuilder: ChainBuilder) = {
    chainBuilder.build(classOf[ResourceDefinitionLoaderListener], configuration)
  }
}
