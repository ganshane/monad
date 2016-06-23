// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.jni.services

import stark.utils.services.ErrorCode


/**
 * jni error code
 */
object JNIErrorCode {
  case object JNI_STATUS_ERROR extends ErrorCode(4001)
}
