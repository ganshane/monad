// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.jni.services

import monad.support.services.ErrorCode


/**
 * jni error code
 */
object JNIErrorCode {
  case object JNI_STATUS_ERROR extends ErrorCode(4001)
}
