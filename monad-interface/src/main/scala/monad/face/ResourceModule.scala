// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face

import monad.face.internal.ResourcesWatcher
import monad.face.services.{ResourceDefinitionLoader, ResourceDefinitionLoaderListener}
import org.apache.tapestry5.ioc.annotations.Marker
import org.apache.tapestry5.ioc.services.{ChainBuilder, RegistryShutdownHub}
import org.apache.tapestry5.ioc.{OrderedConfiguration, ServiceBinder}
import org.apache.tapestry5.services.Core

/**
 * 提供资源模块
 * @author jcai
 */
object ResourceModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[ResourceDefinitionLoader], classOf[ResourcesWatcher]).withId("ResourceDefinitionLoader")
  }

  def contributeRegistryStartup(registryShutdownHub: RegistryShutdownHub,
                                orderedConfiguration: OrderedConfiguration[Runnable], resourceDefinitionLoader: ResourceDefinitionLoader): Unit = {
    //最后启动
    orderedConfiguration.add("resources", new Runnable() {
      override def run(): Unit = {
        resourceDefinitionLoader.getResourceDefinitions
      }
    }, "before:*")
  }

  @Marker(Array(classOf[Core]))
  def buildResourceDefinitionLoaderListener(
                                             configuration: java.util.List[ResourceDefinitionLoaderListener],
                                             chainBuilder: ChainBuilder) = {
    chainBuilder.build(classOf[ResourceDefinitionLoaderListener], configuration)
  }
}
