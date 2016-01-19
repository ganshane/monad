// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.DynamicTraceService
import monad.face.model.JsonApiResponse
import org.apache.tapestry5.ioc.annotations.Inject

/**
 * 得到所有的动态资源列表
 * @author jcai
 */

class GetDynamicResources {
  @Inject
  private var dts: DynamicTraceService = _

  def onActivate = {
    new JsonApiResponse(dts.getDynamicResource)
  }

  //only for test
  private[api] def setDynamicTraceService(dts: DynamicTraceService) {
    this.dts = dts
  }
}
