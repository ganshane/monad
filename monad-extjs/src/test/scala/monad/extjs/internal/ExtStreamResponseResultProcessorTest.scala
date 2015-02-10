// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import java.io.ByteArrayOutputStream

import monad.extjs.model.ExtStreamResponse
import org.apache.tapestry5.services.Response
import org.junit.{Assert, Test}
import org.mockito.Mockito._

/**
 *
 * @author jcai
 */

class ExtStreamResponseResultProcessorTest {
    @Test
    def test_render(){
        val extRequest = new ExtRequestImpl
        extRequest.action="TestPage"
        extRequest.method="testMethod"
        extRequest.tid="asdf"
        val response = mock(classOf[Response])
        //when(response.disableCompression()).thenReturn()
        val baos= new ByteArrayOutputStream
        when(response.getOutputStream
          (ExtStreamResponseResultProcessor.contentType.toString)).
          thenReturn(baos)

        //replay(response)

        val esrrp = new ExtStreamResponseResultProcessor(extRequest,response)
        val esr = new ExtStreamResponse(List("v1","v2"))
        esrrp.processResultValue(esr)

        val result = """
       {"type":"rpc","action":"TestPage","method":"testMethod","tid":"asdf","result":["v1","v2"]}
        """.trim
        Assert.assertEquals(result,baos.toString)

      //verify(response).
    }
}
