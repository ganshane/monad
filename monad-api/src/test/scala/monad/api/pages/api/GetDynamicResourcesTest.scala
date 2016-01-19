// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.DynamicTraceService
import org.junit.Test
import org.mockito.Mockito


/**
 *
 * @author jcai
 */

class GetDynamicResourcesTest {

  @Test
  def test_api() {
    val dts = Mockito.mock(classOf[DynamicTraceService])
    val it = List("test1")
    Mockito.when(dts.getDynamicResource).thenReturn(it.iterator)


    val gdr = new GetDynamicResources
    gdr.setDynamicTraceService(dts)
    gdr.onActivate

    Mockito.verify(dts).getDynamicResource
  }
}
