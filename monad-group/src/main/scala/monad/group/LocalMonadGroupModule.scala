// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group

import monad.core.services.ServiceLifecycleHub
import monad.face.MonadFaceConstants
import monad.face.services.{GroupZookeeperTemplate, GroupServerApi}
import monad.group.internal.local.LocalGroupServiceApiImpl
import monad.group.internal.{MonadGroupManager, MonadGroupUpNotifier}
import monad.support.services.ServiceLifecycle
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.ioc.{Configuration, OrderedConfiguration, ServiceBinder}
import org.apache.tapestry5.services.LibraryMapping
import org.slf4j.LoggerFactory

/**
 *
 * @author jcai
 */
object LocalMonadGroupModule {
  private val logger = LoggerFactory getLogger getClass

  def bind(binder: ServiceBinder) {
    binder.bind(classOf[MonadGroupManager]).withId("MonadGroupManager")
    binder.bind(classOf[MonadGroupUpNotifier]).withId("MonadGroupUpNotifier")
    binder.bind(classOf[GroupServerApi], classOf[LocalGroupServiceApiImpl]).withId("GroupServerApi")
    binder.bind(classOf[GroupZookeeperTemplate]).withId("GroupZookeeperTemplate")
  }

  def contributeComponentClassResolver(configuration: Configuration[LibraryMapping]) {
    configuration.add(new LibraryMapping("group", "monad.group"))
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideServiceLifecycle(configuration: OrderedConfiguration[ServiceLifecycle],
                              monadGroupUpNotifier: MonadGroupUpNotifier,
                              groupZookeeperTemplate: GroupZookeeperTemplate) {
    configuration.add(MonadFaceConstants.LIFE_GROUP_NOTIFIER, monadGroupUpNotifier, "after:" + MonadFaceConstants.LIFE_CLOUD)
    configuration.add(MonadFaceConstants.LIFE_GROUP_ZOOKEEPER, groupZookeeperTemplate, "after:" + MonadFaceConstants.LIFE_GROUP_NOTIFIER)
  }
}
