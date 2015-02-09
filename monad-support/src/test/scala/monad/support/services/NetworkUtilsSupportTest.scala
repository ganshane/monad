// Copyright 2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import org.junit.Test

/**
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class NetworkUtilsSupportTest {
  @Test
  def test_ip: Unit = {
    val support = new NetworkUtilsSupport with LoggerSupport {}
    println(support.ip("10.1.7.*"))
    println(support.ip("127.*"))
  }

}
