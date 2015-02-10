// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import org.junit.{Assert, Test}

class CodingHelperTest {

  @Test
  def test_convert() {
    val i = 12345
    val bytes = CodingHelper.convertAsBytes(i)
    Assert.assertEquals(i, CodingHelper.convertAsInt(bytes))
  }

  @Test
  def test_encoding() {
    val bytes = CodingHelper.EncodeInt32WithBigEndian(1234)
    Assert.assertEquals(1234, CodingHelper.DecodeInt32WithBigEndian(bytes))
  }
}
