// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.config

import java.io.ByteArrayInputStream

import stark.utils.services.XmlLoader
import org.junit.{Assert, Test}

/**
 *
 * @author jcai
 */

class MonadGroupConfigTest {
  @Test
  def test_parse() {
    val xml =
      """
        <monad_group>
            <group id="nanchang">
                <cn_name>南昌</cn_name>
                <api_url>http://10.137.43.92</api_url>
            </group>
        </monad_group>
      """.stripMargin
    val config = XmlLoader.parseXML[MonadGroupConfig](new ByteArrayInputStream(xml.toString().getBytes("UTF-8")), None)
    Assert.assertEquals("nanchang", config.group.id)
    Assert.assertEquals("南昌", config.group.cnName)
  }
}
