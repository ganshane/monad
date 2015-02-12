// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.internal

import java.io.{ByteArrayOutputStream, OutputStream}

import com.google.gson.{JsonArray, JsonObject, JsonPrimitive}
import monad.core.model.JsonApiResponse
import org.apache.tapestry5.services.Response
import org.easymock.EasyMock
import org.junit.{After, Assert, Before, Test}

/**
 *
 * @author jcai
 */

class JsonApiResponseResultProcessorTest {
  private var outputStream: OutputStream = _
  private var processor: JsonApiResponseResultProcessor = _
  private var response: Response = _

  @Test
  def test_json() {
    val json1 = new JsonObject
    json1.addProperty("k1", "v1")
    val json2 = new JsonArray
    json2.add(new JsonPrimitive("v1"))
    val apiResponse = new JsonApiResponse(List(json1, json2))

    processor.processResultValue(apiResponse)

    val r = """
        {"success":true,"status":0,"data":[{"k1":"v1"},["v1"]]}
            """.trim
    Assert.assertEquals(r, outputStream.toString)
  }

  @Test
  def test_Iterable() {
    val apiResponse = new JsonApiResponse(List("v1", "v2").iterator)

    processor.processResultValue(apiResponse)

    val r = """
        {"success":true,"status":0,"data":["v1","v2"]}
            """.trim
    Assert.assertEquals(r, outputStream.toString)
  }

  @Test
  def test_map() {
    val apiResponse = new JsonApiResponse(Map("k" -> "v"))

    processor.processResultValue(apiResponse)

    val r = """
        {"success":true,"status":0,"data":{"k":"v"}}
            """.trim
    Assert.assertEquals(r, outputStream.toString)
  }

  @Test
  def test_simple() {
    val apiResponse = new JsonApiResponse
    apiResponse.status = 2
    apiResponse.msg = "test"

    processor.processResultValue(apiResponse)

    Assert.assertEquals("{\"success\":true,\"status\":2,\"msg\":\"test\",\"data\":{}}", outputStream.toString)
  }

  @Before
  def setup() {
    response = EasyMock.createMock(classOf[Response])
    outputStream = new ByteArrayOutputStream()
    response.disableCompression()
    EasyMock.expect(response.getOutputStream("application/json;charset=UTF-8"))
      .andReturn(outputStream)
    EasyMock.replay(response)

    processor = new JsonApiResponseResultProcessor(response)
  }

  @After
  def verify() {
    EasyMock.verify(response)
  }
}
