// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.egfit.com
package monad.face.internal

import java.io.{BufferedInputStream, ByteArrayInputStream, InputStream, OutputStream}
import java.lang.reflect.Type
import com.google.gson.{JsonSerializationContext, JsonSerializer, JsonElement, GsonBuilder}
import monad.face.model.JsonApiResponse
import org.apache.tapestry5.ContentType
import org.apache.tapestry5.internal.{TapestryInternalUtils, InternalConstants}
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.{ComponentEventResultProcessor, Response}

/**
 * JsonApiResult结果处理
 * @author jcai
 */
class JsonApiResponseResultProcessor(response: Response) extends ComponentEventResultProcessor[JsonApiResponse] {
    private val jsonContentType = new ContentType(InternalConstants.JSON_MIME_TYPE, "UTF-8")
    //增加对JsonElement本身的序列化
    private val gson = new GsonBuilder().registerTypeHierarchyAdapter(classOf[JsonElement],new JsonSerializer[JsonElement] {
        def serialize(src: JsonElement, typeOfSrc: Type, context: JsonSerializationContext) = {
            src
        }
    }).create()

    def processResultValue(apiResult: JsonApiResponse) {
        var os: OutputStream = null
        var is: InputStream = null

        // The whole point is that the response is in the hands of the StreamResponse;
        // if they want to compress the result, they can add their own GZIPOutputStream to
        // their pipeline.

        response.disableCompression()

        try {
            val text = gson.toJson(apiResult)
            val textBytes = text.getBytes(jsonContentType.getCharset)

            val stream = new ByteArrayInputStream(textBytes)
            is = new BufferedInputStream(stream)

            os = response.getOutputStream(jsonContentType.toString)

            TapestryInternalUtils.copy(is, os)

            os.close()
            os = null

            is.close()
            is = null
        }
        finally {
            InternalUtils.close(is)
            InternalUtils.close(os)
        }
    }
}