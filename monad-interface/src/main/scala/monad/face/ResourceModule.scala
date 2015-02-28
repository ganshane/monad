// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face

import monad.face.internal.ResourcesWatcher
import monad.face.services.{ResourceDefinitionLoader, ResourceDefinitionLoaderListener}
import org.apache.tapestry5.ioc.ServiceBinder
import org.apache.tapestry5.ioc.annotations.Marker
import org.apache.tapestry5.ioc.services.ChainBuilder
import org.apache.tapestry5.services.Core

/**
 * 提供资源模块
 * @author jcai
 */
object ResourceModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[ResourceDefinitionLoader], classOf[ResourcesWatcher]).withId("ResourceDefinitionLoader")
  }

  @Marker(Array(classOf[Core]))
  def buildResourceDefinitionLoaderListener(
                                             configuration: java.util.List[ResourceDefinitionLoaderListener],
                                             chainBuilder: ChainBuilder) = {
    chainBuilder.build(classOf[ResourceDefinitionLoaderListener], configuration)
  }
}
