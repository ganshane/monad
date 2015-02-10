// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.internal.{InternalConstants, EmptyEventContext, URLEventContext}
import javax.servlet.http.HttpServletRequest
import scala.collection.JavaConversions._
import org.apache.tapestry5.services.{ContextValueEncoder, ComponentClassResolver, ComponentEventRequestParameters, Request}
import org.slf4j.LoggerFactory
import monad.extjs.services.{ExtRequest, ExtDirectRequestDecoder}

/**
 * 解析传入的json字符串
 * @author jcai
 */
class ExtDirectRequestDecoderImpl(
        componentClassResolver:ComponentClassResolver,
        valueEncoder:ContextValueEncoder,
        servletRequest:HttpServletRequest) extends ExtDirectRequestDecoder{
    /**
     * 解析请求
     */
    override def decodeExtDirectRequest(extRequest:ExtRequest):ComponentEventRequestParameters={
        val activePageName = componentClassResolver.canonicalizePageName(extRequest.getAction.replace('_','/'))
        val eventType =  "extjs_"+ extRequest.getMethod

        var nestedComponentId:String = ""

        if (nestedComponentId == null && eventType == null)
            return null

        val data = extRequest.getData
        val parameters:Array[String]=
            if(data.size()>0 && data.head.isJsonObject){
                data.map(_.toString).toArray
            }else{
                data.map(x=>{
                    if (x.isJsonNull) null else x.getAsString
                }).toArray
            }
        val eventContext = new URLEventContext(valueEncoder, parameters)

        val activationContext = new EmptyEventContext()


        var containingPageName = servletRequest.getParameter(InternalConstants.CONTAINER_PAGE_NAME)

        if (containingPageName == null)
            containingPageName = activePageName
        else
            containingPageName = componentClassResolver.canonicalizePageName(containingPageName)


        new ComponentEventRequestParameters(activePageName, containingPageName, nestedComponentId, eventType,
            activationContext, eventContext)
    }
}
