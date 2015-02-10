// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.mockito.Mockito._
import org.apache.tapestry5.services.ComponentClassResolver
import java.util.ArrayList
import monad.extjs.annotations.ExtDirectMethod
import org.junit.{Assert, Test}
import org.slf4j.LoggerFactory
import org.apache.tapestry5.services.assets.AssetPathConstructor
import java.util

/**
 *
 * @author jcai
 */

class ExtDirectApiResolverImplTest {
    private val logger = LoggerFactory getLogger getClass
    @Test
    def test_ext(){
        val ccr = mock(classOf[ComponentClassResolver])
        val apc = mock(classOf[AssetPathConstructor])
        val pageNames = new util.ArrayList[String]()
        pageNames.add("test/A")
        when(ccr.getPageNames).thenReturn(pageNames)
        when(ccr.resolvePageNameToClassName("test/A")).
          thenReturn(classOf[A].getName)

        when(apc.constructAssetPath("extjs_static","icons")).thenReturn("/icons")
        when(apc.constructAssetPath("extjs_static","examples/ux")).thenReturn("/ux")

        //EasyMock.replay(ccr,apc)

        val resolver = new ExtDirectApiResolverImpl(ccr,apc)
        val json=resolver.getActions
        logger.debug("json:{}",json.toString)
        Assert.assertTrue(json.has("test_A"))
        val methods = json.getAsJsonArray("test_A")
        Assert.assertEquals(2,methods.size)
        logger.debug("js:\n{}",resolver.toJs)


      verify(ccr).getPageNames
      verify(apc).constructAssetPath("extjs_static","icons")
    }
    class A{
        @ExtDirectMethod
        def test_method(){

        }
        @ExtDirectMethod
        def test_method2(arg1:String){
        }
    }
}
