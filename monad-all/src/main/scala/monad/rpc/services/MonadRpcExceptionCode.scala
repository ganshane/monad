package monad.rpc.services

import monad.support.services.ErrorCode

/**
 *
 * @author jcai
 */
object MonadRpcExceptionCode {

  case object OVER_RETRY_FIND_SERVER_TIMEOUT extends ErrorCode(7001)

  case object ALL_SERVER_DOWN extends ErrorCode(7002)

  case object WAIT_REMOTE_TIMEOUT extends ErrorCode(7003)

  case object REMAIN_MESSAGE_UNREAD extends ErrorCode(7004)

  case object OVER_BIND_COUNT extends ErrorCode(7005)

  case object UNABLE_CONNECT_REMOTE_SERVER extends ErrorCode(7006)

  case object INSERT_QUEUE_TIMEOUT extends ErrorCode(7007)

  case object FAIL_WRITE_CHANNEL extends ErrorCode(7008)

  case object SERVICE_NOT_FOUND extends ErrorCode(7009)

  case object MERGER_NOT_FOUND extends ErrorCode(7010)

  case object ERROR_OCCUR_ON_SERVER extends ErrorCode(7011)

  case object SERVER_NOT_FOUND extends ErrorCode(7012)

}
