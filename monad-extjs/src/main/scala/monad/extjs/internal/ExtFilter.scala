// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.apache.tapestry5.services.{HttpServletRequestHandler, HttpServletRequestFilter}
import monad.extjs.MonadExtjsConstants
import java.io.InputStreamReader
import org.slf4j.LoggerFactory
import monad.extjs.services.{ExtRequest, ExtRequestGlobals}
import io.Source
import com.google.gson.{JsonArray, JsonStreamParser, JsonObject}
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * ext js request filter
 * @author jcai
 */

class ExtFilter(extRequestGlobals:ExtRequestGlobals) extends HttpServletRequestFilter{
    //logger
    private val logger = LoggerFactory getLogger getClass

    override def service(request: HttpServletRequest, response: HttpServletResponse, handler: HttpServletRequestHandler):Boolean ={
        //检查request的HEADER
        val extjsHeader = request.getHeader(MonadExtjsConstants.EXT_JS_REQUEST_HEADER)
        if(extjsHeader == null){
            return handler.service(request,response)
        }
        //分析传入的JSON字符串
        val jsonString = Source.
          fromInputStream(request.getInputStream,"UTF-8").
          getLines().
          mkString("")
        if(InternalUtils.isBlank(jsonString)){
            return handler.service(request,response)
        }
        logger.debug("receive json string:{}",jsonString)
        val parser = new JsonStreamParser(jsonString)
        if(! parser.hasNext){
            return handler.service(request,response)
        }
        val jsonElement = parser.next()
        val json= if (jsonElement.isJsonArray){
            jsonElement.getAsJsonArray.get(0).getAsJsonObject
        }else{
            jsonElement.getAsJsonObject
        }

        val extRequest = new ExtRequestImpl
        extRequest.requestType = "rpc"
        extRequest.action =  json.get("action").getAsString
        extRequest.method = json.get("method").getAsString
        extRequest.tid = json.get("tid").getAsString
        if(json.has("data") && !json.get("data").isJsonNull){
            extRequest.data = json.get("data").getAsJsonArray
        }else extRequest.data = new JsonArray

        extRequestGlobals.storeExtRequest(extRequest)

        handler.service(request,response)
    }
}
