// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.RelationService
import monad.core.model.JsonApiResponse
import org.apache.tapestry5.ioc.annotations.Inject

/**
 * 得到所有关系定义
 * @author jcai
 */

class GetRelations {
  @Inject
  private var relationService: RelationService = _

  def onActivate = {
    new JsonApiResponse(relationService.findRelations)
  }

  //only for test
  private[api] def setRelationService(service: RelationService) {
    this.relationService = service
  }
}
