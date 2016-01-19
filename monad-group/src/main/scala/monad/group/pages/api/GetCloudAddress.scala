// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
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
    new JsonApiResponse(config.zk.address)
  }
}
