// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.services.assets.AssetPathConstructor
import org.mockito.Mockito._
import collection.JavaConversions._
import org.junit.{Assert, Test}

/**
 *
 * @author jcai
 */

class FileSystemAssetAliasManagerImplTest {
    @Test
    def test_toClientURL(){
        val apc = mock(classOf[AssetPathConstructor])
        when(apc.constructAssetPath("extjs_static","ext-all.js")).thenReturn("/extjs/ext-all.js")

        //replay(apc)
        val config = Map("extjs_static"->"/data/extjs")
        val fsaam = new FileSystemAssetAliasManagerImpl(apc,mapAsJavaMap(config))
        val resourcePath = "/data/extjs/ext-all.js"
        val clientUrl = fsaam.toClientURL(resourcePath)
        Assert.assertEquals("/extjs/ext-all.js",clientUrl)

        verify(apc).constructAssetPath("extjs_static","ext-all.js")
    }
}
