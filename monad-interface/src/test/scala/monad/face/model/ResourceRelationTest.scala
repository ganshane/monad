// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import monad.support.services.XmlLoader
import org.junit.{Assert, Test}

/**
 * resource relation test
 * @author jcai
 */
class ResourceRelationTest {
  @Test
  def test_resource() {
    val rr = XmlLoader.parseXML[ResourceRelation](
      getClass.getResourceAsStream("/relations.xml"), None)

    Assert.assertEquals(1, rr.relations.size())
    val r = rr.relations.get(0)
    Assert.assertEquals("th", r.name)
    Assert.assertEquals("同户", r.cnName)
    Assert.assertEquals("czrk", r.resource)

    Assert.assertEquals(1, r.properties.size())
    val p = r.properties.get(0)
    Assert.assertEquals("sfzh", p.name)
    Assert.assertEquals("th1", p.traitProperty)
  }
}
