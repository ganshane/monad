// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face

import monad.core.services.ServiceLifecycleHub
import monad.face.internal.remote.RemoteGroupServiceApiImpl
import monad.face.services.{GroupServerApi, GroupZookeeperTemplate}
import monad.support.internal.HttpRestClientImpl
import monad.support.services.{HttpRestClient, ServiceLifecycle}
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.ioc.{OrderedConfiguration, ServiceBinder}

/**
 *
 * @author jcai
 */
object RemoteGroupModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[GroupZookeeperTemplate]).withId("GroupZookeeperTemplate")
    binder.bind(classOf[HttpRestClient], classOf[HttpRestClientImpl]).withId("HttpRestClient")
    binder.bind(classOf[GroupServerApi], classOf[RemoteGroupServiceApiImpl]).withId("GroupServerApi")
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideServiceLifecycle(configuration: OrderedConfiguration[ServiceLifecycle],
                              groupZookeeperTemplate: GroupZookeeperTemplate) {
    configuration.add(MonadFaceConstants.LIFE_GROUP_ZOOKEEPER, groupZookeeperTemplate,
      "after:" + MonadFaceConstants.LIFE_GROUP_NOTIFIER,
      "after:" + MonadFaceConstants.LIFE_CLOUD
    )
  }
}
