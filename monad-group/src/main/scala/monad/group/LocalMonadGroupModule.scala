// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group

import monad.face.services.{GroupServerApi, GroupZookeeperTemplate}
import monad.group.internal.local.LocalGroupServiceApiImpl
import monad.group.internal.{MonadGroupManager, MonadGroupUpNotifier}
import org.apache.tapestry5.ioc.{Configuration, ServiceBinder}
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

}
