// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.group.pages.api

import monad.core.config.GroupConfigSupport
import monad.core.model.{GroupConfig, JsonApiResponse}
import org.apache.tapestry5.ioc.annotations.Inject

/**
 * 得到自己组的配置
 * @author jcai
 */
class GetSelfGroupConfig {
  @Inject
  private var monadGroupConfig: GroupConfigSupport = _

  def onActivate = {
    val groupConfig = new GroupConfig
    groupConfig.id = monadGroupConfig.group.id
    groupConfig.cnName = monadGroupConfig.group.cnName
    groupConfig.apiUrl = monadGroupConfig.group.apiUrl
    new JsonApiResponse(groupConfig)
  }
}
