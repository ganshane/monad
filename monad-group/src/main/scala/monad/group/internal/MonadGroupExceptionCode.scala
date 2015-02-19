package monad.group.internal

import monad.support.services.ErrorCode

/**
 * group模块的异常定义
 * @author jcai
 */
object MonadGroupExceptionCode {

  case object FAIL_GET_SELF_GROUP_CONFIG extends ErrorCode(4001)

  case object MISSING_RESOURCE_NAME extends ErrorCode(4002)

  case object FAIL_CONNECT_GROUP_SERVER extends ErrorCode(4003)

}
