// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.group.pages.api

import monad.core.config.GroupConfigSupport
import monad.core.model.JsonApiResponse
import monad.group.internal.MonadGroupUpNotifier
import org.apache.tapestry5.ioc.annotations.Inject

/**
 * 得到其他组
 * @author jcai
 */
class GetOtherGroups {
  @Inject
  private var monadClusterManager: MonadGroupUpNotifier = _
  @Inject
  private var monadGroupConfig: GroupConfigSupport = _

  def onActivate = {
    new JsonApiResponse(monadClusterManager.getLiveGroups.filterNot(_.id == monadGroupConfig.group.id))
  }
}
