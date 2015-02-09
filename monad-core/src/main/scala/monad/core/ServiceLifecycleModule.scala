// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core

import monad.core.internal.ServiceLifecycleHubImpl
import monad.core.services.ServiceLifecycleHub
import org.apache.tapestry5.ioc.annotations.{Local, Symbol}
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.tapestry5.ioc.{OrderedConfiguration, ServiceBinder}

/**
 * service lifecycle
 */
object ServiceLifecycleModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[ServiceLifecycleHub], classOf[ServiceLifecycleHubImpl]).
      withId("ServiceLifecycleHub").eagerLoad()
  }

  def contributeRegistryStartup(configuration: OrderedConfiguration[Runnable],
                                @Local serviceHub: ServiceLifecycleHub,
                                registryShutdownHub: RegistryShutdownHub,
                                @Symbol(MonadCoreSymbols.SERVER_HOME) serverHome: String) {
    registryShutdownHub.addRegistryWillShutdownListener(new Runnable {
      def run() {
        serviceHub.shutdown()
      }
    })
  }
}
