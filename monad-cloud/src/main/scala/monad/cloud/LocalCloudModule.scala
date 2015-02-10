// Copyright 2012,2013,2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud

import monad.cloud.internal.CloudServerImpl
import monad.cloud.services.CloudServer
import monad.core.services.ServiceLifecycleHub
import monad.support.services.ServiceLifecycle
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.ioc.{OrderedConfiguration, ServiceBinder}

/**
 * local cloud module
 * @author jcai
 */
object LocalCloudModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[CloudServer], classOf[CloudServerImpl]).withId("CloudServer")
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideObjectLifecycle(configuration: OrderedConfiguration[ServiceLifecycle],
                             cloudServer: CloudServer) {
    configuration.add("cloud", cloudServer, "before:*")
  }
}
