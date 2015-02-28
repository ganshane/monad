// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.pages.api

import monad.face.config.GroupConfigSupport
import monad.face.model.{JsonApiResponse, GroupConfig}
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
