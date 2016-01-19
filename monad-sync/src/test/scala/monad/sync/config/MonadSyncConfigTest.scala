// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.config

import monad.support.services.XmlLoader
import org.junit.{Assert, Test}

/**
 *
 * @author jcai
 */
class MonadSyncConfigTest {
  @Test
  def test_parse {
    val config = XmlLoader.parseXML[MonadSyncConfig](getClass.getResourceAsStream("/test-sync.xml"), None)
    println(XmlLoader.toXml(config))
    Assert.assertEquals("${server_home}/log", config.logFile)
    Assert.assertEquals("http://localhost:9081/api", config.groupApi)
    Assert.assertEquals(2, config.sync.nodes.size())
  }
}
