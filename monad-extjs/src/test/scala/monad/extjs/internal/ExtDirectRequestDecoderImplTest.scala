// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.services.{ContextValueEncoder, ComponentClassResolver}
import monad.extjs.services.ExtRequest
import javax.servlet.http.HttpServletRequest
import org.mockito.Mockito._
import com.google.gson.JsonParser
import org.junit.{Assert, Test}

/**
 *
 * @author jcai
 */
class ExtDirectRequestDecoderImplTest {
    @Test
    def test_decoder(){
        
        val componentClassResolver = mock(classOf[ComponentClassResolver])
        val valueEncoder = mock(classOf[ContextValueEncoder])
        val extRequest=mock(classOf[ExtRequest])
        val servletRequest = mock(classOf[HttpServletRequest])

        when(extRequest.getAction).thenReturn("TestPage")
        when(componentClassResolver.canonicalizePageName("TestPage")).thenReturn("TestPage")

        when(extRequest.getMethod).thenReturn("testMethod")
        val parser = new JsonParser
        val arr=parser.parse("['v1','v2',2]").getAsJsonArray

        when(extRequest.getData).thenReturn(arr)
        when(servletRequest.getParameter("t:cp")).thenReturn(null)

        //replay(componentClassResolver,valueEncoder,extRequest,servletRequest)

        val edrd = new ExtDirectRequestDecoderImpl(componentClassResolver,
            valueEncoder,
            servletRequest)
        val cerp = edrd.decodeExtDirectRequest(extRequest)
        Assert.assertEquals("TestPage",cerp.getActivePageName)
        Assert.assertEquals("TestPage",cerp.getContainingPageName)
        val context = cerp.getEventContext
        Assert.assertEquals(3,context.getCount)

        verify(componentClassResolver,valueEncoder,extRequest,servletRequest)
    }
}
