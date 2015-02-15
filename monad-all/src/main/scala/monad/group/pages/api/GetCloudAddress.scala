// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.group.pages.api

import monad.face.model.JsonApiResponse
import monad.group.config.MonadGroupConfig
import org.apache.tapestry5.ioc.annotations.Inject

/**
 * 得到云的地址
 * @author jcai
 */
class GetCloudAddress {
  @Inject
  private var config: MonadGroupConfig = _

  def onActivate = {
    new JsonApiResponse(config.cloudServer)
  }
}
