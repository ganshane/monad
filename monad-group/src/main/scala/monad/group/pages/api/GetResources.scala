// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.pages.api

import monad.face.config.GroupConfigSupport
import monad.face.model.JsonApiResponse
import monad.group.internal.MonadGroupUpNotifier
import org.apache.tapestry5.annotations.RequestParameter
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * 获取所有的资源的定义
 * @author jcai
 */
class GetResources {
  @Inject
  private var monadClusterManager: MonadGroupUpNotifier = _
  @Inject
  private var monadGroupConfig: GroupConfigSupport = _

  def onActivate(@RequestParameter(value = "group", allowBlank = true) group: String) = {
    var groupName = group
    if (InternalUtils.isBlank(groupName)) {
      groupName = monadGroupConfig.group.id
    }
    new JsonApiResponse(monadClusterManager.findResourcesContent(groupName).
      filterNot(x =>
      x.contains("type=\"Data\"") || x.contains("save=\"false\"") || x.contains("share=\"false\"")
      ))
  }
}
