// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import monad.support.services.ErrorCode

/**
 * monad核心模块对异常的定义
 * @author jcai
 */
object MonadFaceExceptionCode {

  case object ANALYZER_TYPE_IS_NULL extends ErrorCode(3001)

  case object RESOURCE_NOT_FOUND extends ErrorCode(3002)

  case object OBJECT_NOT_LIVE extends ErrorCode(3003)

  case object FAIL_GET_SELF_GROUP_CONFIG extends ErrorCode(3004)

  case object FAIL_CONNECT_GROUP_SERVER extends ErrorCode(3005)

  case object INDEX_TYPE_NOT_SUPPORTED extends ErrorCode(3006)

}
