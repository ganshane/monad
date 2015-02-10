// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.junit.Test
import org.mockito.Mockito._
import monad.extjs.services.ExtRequestGlobals
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tapestry5.services.HttpServletRequestHandler
import monad.extjs.MonadExtjsConstants
import java.io.ByteArrayInputStream
import javax.servlet.ServletInputStream

/**
 *
 * @author jcai
 */
class ExtFilterTest {
    @Test
    def test_filter(){
        val request = mock(classOf[HttpServletRequest])
        val response = mock(classOf[HttpServletResponse])
        val handler = mock(classOf[HttpServletRequestHandler])

        when(request.getHeader(MonadExtjsConstants.EXT_JS_REQUEST_HEADER)).
          thenReturn("4.1")
        val servletStream = new ServletInputStream {
            val is = new ByteArrayInputStream(
                "{action:'TestPage',method:'testMethod',tid:'12',data:['v1','v2']}".getBytes("UTF-8"))
            def read() = is.read()
        }
        when(request.getInputStream).thenReturn(servletStream)
        when(handler.service(request,response)).thenReturn(true)
        
        //replay(request,response,handler)

        val extRequestGlobals = new ExtRequestGlobalsImpl
        val filter = new ExtFilter(extRequestGlobals)
        filter.service(request,response,handler)

        verify(request,response,handler)
    }
}
