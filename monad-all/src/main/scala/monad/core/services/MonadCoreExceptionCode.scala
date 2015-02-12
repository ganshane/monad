package monad.core.services

import monad.support.services.ErrorCode

/**
 * monad核心模块对异常的定义
 * @author jcai
 */
object MonadCoreExceptionCode {

  case object ANALYZER_TYPE_IS_NULL extends ErrorCode(3001)

  case object RESOURCE_NOT_FOUND extends ErrorCode(3002)

  case object OBJECT_NOT_LIVE extends ErrorCode(3003)

}
