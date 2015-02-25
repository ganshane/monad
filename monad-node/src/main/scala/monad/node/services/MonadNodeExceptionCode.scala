package monad.node.services

import monad.support.services.ErrorCode

/**
 * node的异常编码
 * @author jcai
 */
object MonadNodeExceptionCode {

  case object RESOURCE_SEARCH_NO_FOUND extends ErrorCode(5001)

  case object QUERY_TIMEOUT extends ErrorCode(5002)

  case object HIGH_CONCURRENT extends ErrorCode(5003)

  case object SEARCHER_CLOSING extends ErrorCode(5004)

  case object UNABLE_UNMAP_BUFFER extends ErrorCode(5005)

  case object INVALID_INDEX_PAYLOAD extends ErrorCode(5006)

  case object OVERFLOW_DIRECT_BUFFER extends ErrorCode(5007)

  case object FAIL_TO_ALLOCATE_MEMORY extends ErrorCode(5008)

  case object FAIL_TO_LOCK_MEMORY extends ErrorCode(5009)

  case object NOSQL_LOG_DATA_IS_NULL extends ErrorCode(5010)

  case object INDEXER_WILL_SHUTDOWN extends ErrorCode(5011)

  case object FAIL_TO_PARSE_QUERY extends ErrorCode(5012)

  case object OBJECT_ID_IS_NULL extends ErrorCode(5013)

  case object OVERFLOW_RESOURCE_RANGE extends ErrorCode(5014)

}
