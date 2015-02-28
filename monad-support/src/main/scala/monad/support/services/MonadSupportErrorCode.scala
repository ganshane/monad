// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

/**
 * define support module error code
 */
object MonadSupportErrorCode {

  object FAIL_PARSE_XML extends ErrorCode(1001)

  object ZK_FAIL_TO_CREATE extends ErrorCode(1002)

  object ZK_NOT_BE_INITIALIZED extends ErrorCode(1003)

  object WAITING_SERVER_INIT_TIMEOUT extends ErrorCode(1004)

  object SERVICE_HAS_INITIALIZED extends ErrorCode(1005)

  object SERVER_FAIL_BIND extends ErrorCode(1006)
  object TIMEOUT_TO_GET_OBJECT extends ErrorCode(1007)

}
