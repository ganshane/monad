// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito._
import org.apache.tapestry5.internal.services.ResourceStreamer
import org.apache.tapestry5.ioc.Resource
import org.apache.tapestry5.services.{Request, Response}
import monad.extjs.MonadExtjsConstants._

/**
 *
 * @author jcai
 */

class CacheResourceStreamerImplTest {
    @Test
    def test_no_cache(){
        val response = mock(classOf[Response])
        val request = mock(classOf[Request])
        val delegate = mock(classOf[ResourceStreamer])
        val resource = mock(classOf[Resource])

        response.setHeader(CACHE_CONTROL,CACHE_CONTROL_VALUE)
        when(request.getHeader(CACHE_HEADER)).thenReturn(null)
        delegate.streamResource(resource)

        //replay(request,response,delegate,resource)

        val crs = new CacheResourceStreamerImpl(true,request,response,delegate)
        crs.streamResource(resource)

        verify(request).getHeader(CACHE_HEADER)
    }
    @Test
    def test_cache_content(){
        val response = mock(classOf[Response])
        val request = mock(classOf[Request])
        val delegate = mock(classOf[ResourceStreamer])
        val resource = mock(classOf[Resource])

        response.setHeader(CACHE_CONTROL,CACHE_CONTROL_VALUE)
        when(request.getHeader(CACHE_HEADER)).thenReturn("123")
        response.setStatus(304)

        //replay(request,response,delegate,resource)

        val crs = new CacheResourceStreamerImpl(true,request,response,delegate)
        crs.streamResource(resource)

        verify(request,times(1)).getHeader(CACHE_HEADER)
    }
}
