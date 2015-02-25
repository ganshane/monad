// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.config

import monad.support.services.XmlLoader
import org.junit.{Assert, Test}

/**
 *
 * @author jcai
 */
class MonadApiConfigTest {
  @Test
  def test_parse() {
    val config = XmlLoader.parseXML[MonadApiConfig](getClass.getResourceAsStream("/test-monad-api.xml"), None)
    Assert.assertEquals(config.groupApi, "http://localhost:9080/api")
  }
}
