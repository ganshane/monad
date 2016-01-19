// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.support.services.ErrorCode

/**
 * monad sync exception
 * @author jcai
 */
object MonadSyncExceptionCode {

  case object JDBC_ERROR extends ErrorCode(8001)

  case object INCREMENT_COLUMN_NOT_DEFINED extends ErrorCode(8002)

  case object OBJECT_ID_IS_NULL extends ErrorCode(8003)

  case object PRIMARY_KEY_VALUE_IS_NULL extends ErrorCode(8004)

  case object INVALIDATE_RESOURCE_CONFIG extends ErrorCode(8005)

  case object FAIL_SAVE_DATA extends ErrorCode(8006)

  case object FAIL_UPDATE_TIMESTAMP extends ErrorCode(8007)

  case object TARGET_RESOURCE_NOT_EXIST extends ErrorCode(8008)

  case object SAVER_NOT_FOUND extends ErrorCode(8009)

  case object DUPLICATE_INCREMENT_COLUMN extends ErrorCode(8010)

  case object DUPLICATE_OBJECT_ID_COLUMN extends ErrorCode(8011)

  case object FAIL_READ_DATA_FROM_DB extends ErrorCode(8012)

  case object SQL_UNSUPPORTED extends ErrorCode(8013)

  case object DUPLICATE_PARTITION_DEFINITION extends ErrorCode(8014)

  case object PARTITION_NOT_FOUND extends ErrorCode(8015)

}
