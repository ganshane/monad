package monad.face.services

import monad.support.services.ErrorCode

/**
 * monad核心模块对异常的定义
 * @author jcai
 */
object MonadFaceExceptionCode {
  case object ANALYZER_TYPE_IS_NULL extends ErrorCode(3001)

}
