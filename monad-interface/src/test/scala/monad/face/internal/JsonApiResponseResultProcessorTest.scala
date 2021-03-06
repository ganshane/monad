// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.internal

import java.io.{ByteArrayOutputStream, OutputStream}

import com.google.gson.{JsonArray, JsonObject, JsonPrimitive}
import monad.face.model.JsonApiResponse
import org.apache.tapestry5.services.Response
import org.junit.{After, Assert, Before, Test}
import org.mockito.Mockito

/**
 *
 * @author jcai
 */

class JsonApiResponseResultProcessorTest {
    private var outputStream:OutputStream = _
    private var processor:JsonApiResponseResultProcessor = _
    private var response:Response = _

    @Test
    def test_json(){
        val json1 = new JsonObject
        json1.addProperty("k1","v1")
        val json2 = new JsonArray
        json2.add(new JsonPrimitive("v1"))
        val apiResponse = new JsonApiResponse(List(json1,json2))

        processor.processResultValue(apiResponse)

        val r = """
        {"success":true,"status":0,"data":[{"k1":"v1"},["v1"]]}
        """.trim
        Assert.assertEquals(r,outputStream.toString)
    }
    @Test
    def test_Iterable(){
        val apiResponse = new JsonApiResponse(List("v1","v2").iterator)

        processor.processResultValue(apiResponse)

        val r = """
        {"success":true,"status":0,"data":["v1","v2"]}
        """.trim
        Assert.assertEquals(r,outputStream.toString)
    }
    @Test
    def test_map(){
        val apiResponse = new JsonApiResponse(Map("k"->"v"))

        processor.processResultValue(apiResponse)

        val r = """
        {"success":true,"status":0,"data":{"k":"v"}}
        """.trim
        Assert.assertEquals(r,outputStream.toString)
    }
    @Test
    def test_simple(){
      val apiResponse = new JsonApiResponse(None)
        apiResponse.status = 2
        apiResponse.msg="test"

        processor.processResultValue(apiResponse)

        Assert.assertEquals("{\"success\":true,\"status\":2,\"msg\":\"test\",\"data\":{}}",outputStream.toString)
    }
    @Before
    def setup(){
        response = Mockito.mock(classOf[Response])
        outputStream = new ByteArrayOutputStream()
        response.disableCompression()
        Mockito.when(response.getOutputStream("application/json;charset=UTF-8"))
          .thenReturn(outputStream)

        processor = new JsonApiResponseResultProcessor(response)
    }
    @After
    def verify(){
      Mockito.verify(response,Mockito.times(1)).getOutputStream("application/json;charset=UTF-8")
    }
}
