// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mockito._
import org.apache.tapestry5.internal.services.{AssetResourceLocator, ResourceStreamer}
import org.apache.tapestry5.services.{Response, Request}
import org.apache.tapestry5.ioc.internal.util.ClasspathResource
import org.apache.tapestry5.ioc.Resource

/**
 *
 * @author jcai
 */

class CoffeeScriptAssetRequestHandlerTest {
    @Test
    def test_compile(){
        val streamer = mock(classOf[ResourceStreamer])
        val arl = mock(classOf[AssetResourceLocator])
        val basePath = "/extjs"
        val file = "test.coffee"

        val request = mock(classOf[Request])
        val response = mock(classOf[Response])

        when(arl.findClasspathResourceForPath(basePath+"/"+file)).
          thenReturn(new ClasspathResource("test.coffee"))

        //streamer.streamResource(Matchers.anyObject[Resource]())

        //replay(streamer,arl,request,response)
        val csarh = new CoffeeScriptAssetRequestHandler(streamer,arl,basePath)
        csarh.handleAssetRequest(request, response, file)

      verify(streamer,times(1)).streamResource(Matchers.anyObject[Resource]())

    }
}
