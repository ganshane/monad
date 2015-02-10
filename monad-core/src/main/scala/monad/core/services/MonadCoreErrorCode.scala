// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import monad.support.services.ErrorCode

/**
 * monad core error code
 */
object MonadCoreErrorCode {

  object FAIL_TO_GET_LOCAL_KV_SNAPSHOT extends ErrorCode(2001)

  object VERSION_EXISTS_IN_KV extends ErrorCode(2002)

  object SERVER_FAIL_BIND extends ErrorCode(2003)

  object INVALID_LOCAL_VERSION extends ErrorCode(2004)

  object HEART_PATH_EXISTS extends ErrorCode(2005)

  object IP_NOT_FOUND extends ErrorCode(2006)

}
