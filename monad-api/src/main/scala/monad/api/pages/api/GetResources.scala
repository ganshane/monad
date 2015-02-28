// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.RelationService
import monad.face.model.JsonApiResponse
import org.apache.tapestry5.ioc.annotations.Inject

/**
 * 获取所有的资源的定义
 * @author jcai
 */
class GetResources {
  @Inject
  private var relationService: RelationService = _

  def onActivate = {
    new JsonApiResponse(relationService.getResources.
      filterNot(x => x.contains("type=\"Data\"") || x.contains("save=\"false\"")))
  }
}
