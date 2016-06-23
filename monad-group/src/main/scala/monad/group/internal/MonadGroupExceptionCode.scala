// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.internal

import stark.utils.services.ErrorCode

/**
 * group模块的异常定义
 * @author jcai
 */
object MonadGroupExceptionCode {

  case object FAIL_GET_SELF_GROUP_CONFIG extends ErrorCode(4001)

  case object MISSING_RESOURCE_NAME extends ErrorCode(4002)

  case object FAIL_CONNECT_GROUP_SERVER extends ErrorCode(4003)

}
