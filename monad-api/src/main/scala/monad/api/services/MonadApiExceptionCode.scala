// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.services

import monad.support.services.ErrorCode

/**
 * API模块部分针对异常的定义
 * @author jcai
 */
object MonadApiExceptionCode {

  case object FAIL_GET_RESULT extends ErrorCode(2001)

  case object FAIL_FIND_DYNAMIC_DEFINITION extends ErrorCode(2002)

  case object HIGH_CONCURRENT extends ErrorCode(2003)

  case object ID_CARD_IS_NULL extends ErrorCode(2004)

  case object RESOURCE_NOT_FOUND extends ErrorCode(2005)

  case object MISSING_RESOURCE_PARAMETER extends ErrorCode(2006)

  case object MISSING_RELATION_PARAMETER extends ErrorCode(2007)

  case object RELATION_NOT_FOUND extends ErrorCode(2008)

  case object MISSING_RELATION_PROPERTY_PARAMETER extends ErrorCode(2009)

  case object MISSING_QUERY_PARAMETER extends ErrorCode(2010)

  case object INVALIDATE_DYNAMIC_RESOURCE extends ErrorCode(2011)

  case object INVALIDATE_OBJECT_ID extends ErrorCode(2012)

}
