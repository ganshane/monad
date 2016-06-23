// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.config

import stark.utils.services.XmlLoader
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
