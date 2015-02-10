// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import java.lang.Throwable
import org.apache.tapestry5.internal.services.{PageResponseRenderer, RequestPageCache, DefaultRequestExceptionHandler}
import org.apache.tapestry5.SymbolConstants
import org.apache.tapestry5.ioc.annotations.Symbol
import monad.extjs.services.ExtRequest
import monad.extjs.MonadExtjsConstants
import org.apache.tapestry5.services.{RequestExceptionHandler, Response, Request}
import org.slf4j.{LoggerFactory, Logger}

/**
 * ext request exception handler
 * @author jcai
 */

class ExtRequestExceptionHandler(
        response:Response,
        request:Request,
        extRequest:ExtRequest) extends RequestExceptionHandler{
    private var logger = LoggerFactory getLogger getClass
    private var handler:RequestExceptionHandler = _
    //设置原始的handler
    def setDelegate(handler:RequestExceptionHandler){
        this.handler = handler
    }
    override def handleRequestException(exception: Throwable) {
        if(request.getHeader(MonadExtjsConstants.EXT_JS_REQUEST_HEADER) == null){
            handler.handleRequestException(exception)
        }else{
            logger.error(exception.getMessage,exception)
            ExtStreamResponseResultProcessor.render(extRequest,response,exception)
        }
    }
}
