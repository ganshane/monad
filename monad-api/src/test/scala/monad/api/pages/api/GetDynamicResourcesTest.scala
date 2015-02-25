// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.DynamicTraceService
import org.easymock.EasyMock
import org.junit.Test


/**
 *
 * @author jcai
 */

class GetDynamicResourcesTest {

  @Test
  def test_api() {
    val dts = EasyMock.createMock(classOf[DynamicTraceService])
    val it = List("test1")
    EasyMock.expect(dts.getDynamicResource).andReturn(it.iterator)

    EasyMock.replay(dts)

    val gdr = new GetDynamicResources
    gdr.setDynamicTraceService(dts)
    gdr.onActivate

    EasyMock.verify(dts)
  }
}
