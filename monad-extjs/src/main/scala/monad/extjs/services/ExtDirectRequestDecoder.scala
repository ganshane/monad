// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.services

import org.apache.tapestry5.services.{ComponentEventRequestParameters, Request}

/**
 * 解析ext的请求
 * @author jcai
 */
trait ExtDirectRequestDecoder{
    /**
     * 解析请求
     */
    def decodeExtDirectRequest(extRequest:ExtRequest):ComponentEventRequestParameters
}
