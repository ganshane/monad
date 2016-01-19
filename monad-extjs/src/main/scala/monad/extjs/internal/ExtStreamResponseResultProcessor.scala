// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import monad.extjs.model.ExtStreamResponse
import org.apache.tapestry5.services.{Response, ComponentEventResultProcessor}
import org.apache.tapestry5.internal.TapestryInternalUtils
import org.apache.tapestry5.ContentType
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import java.io.{ByteArrayInputStream, BufferedInputStream, InputStream, OutputStream}
import monad.extjs.services.ExtRequest
import org.slf4j.LoggerFactory
import com.google.gson.{JsonObject, JsonArray}

import scala.compat.Platform

/**
 * ext stream response result processor
 * @author jcai
 */
object ExtStreamResponseResultProcessor{
    private val logger = LoggerFactory getLogger getClass
    private [internal] val contentType = new ContentType("text/plain","UTF-8")
    def render(extRequest:ExtRequest,response:Response,exception: Throwable){
        render(extRequest,response,json=>{
            json.addProperty("type","exception")
            json.addProperty("message",""+exception.getMessage)
            json.addProperty("where",exception.getStackTrace().mkString("",Platform.EOL,Platform.EOL))
        })
    }
    def render(extRequest:ExtRequest,response:Response,value:ExtStreamResponse){
        render(extRequest,response,json=>{
            if(value.result != null)
                json.add("result",
                    value.result match{
                        case jo:JsonObject=>
                            jo
                        case other=>
                            GsonClassConverter.toJSON(value.result)
                    }
                )
            else
                json.add("result",new JsonObject)

        })
    }
    def render(extRequest:ExtRequest,response:Response,f:(JsonObject)=>Unit){
        var os:OutputStream  = null
        var is:InputStream  = null

        // The whole point is that the response is in the hands of the StreamResponse;
        // if they want to compress the result, they can add their own GZIPOutputStream to
        // their pipeline.

        response.disableCompression()

        try
        {
            val json = new JsonObject
            json.addProperty("type","rpc")
            json.addProperty("action",extRequest.getAction)
            json.addProperty("method",extRequest.getMethod)
            json.addProperty("tid",extRequest.getTid)

            f(json)

            val jsonStr = json.toString
            logger.debug("render js:{}",jsonStr)
            val textBytes = jsonStr.getBytes(contentType.getCharset)

            val bais = new ByteArrayInputStream(textBytes)

            is = new BufferedInputStream(bais)

            os = response.getOutputStream(contentType.toString)

            TapestryInternalUtils.copy(is, os)

            os.close()
            os = null

            is.close()
            is = null
        }
        finally
        {
            InternalUtils.close(is)
            InternalUtils.close(os)
        }
    }
}
class ExtStreamResponseResultProcessor(extRequest:ExtRequest,response:Response)
    extends ComponentEventResultProcessor[ExtStreamResponse]{
    def processResultValue(value: ExtStreamResponse) {
        ExtStreamResponseResultProcessor.render(extRequest,response,value)
    }
}
