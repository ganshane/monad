// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal.support

import java.nio.ByteBuffer

import monad.face.services.DataTypeUtils
import org.junit.{Assert, Test}

/**
 * 采用内部的buffer
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class JNAIdBufferTest {
  @Test
  def test_jnabuffer {
    val buffer = new JNAIdBuffer(5, 8)
    val data = ByteBuffer.allocate(8)
    data.mark()
    data.reset()
    data.putInt(123).putInt(321)
    buffer.put(data.array())

    data.reset()
    data.putInt(234).putInt(432)
    buffer.put(data.array())

    data.reset()
    data.putInt(345).putInt(543)
    buffer.put(data.array())


    buffer.put(111)
    buffer.put(121)

    var l = 131L
    l += 141 << 32L
    buffer.put(l)



    Assert.assertEquals(234, buffer.getInt(1))
    Assert.assertEquals(234, DataTypeUtils.convertAsInt(buffer.apply(1)))
    Assert.assertEquals(432, buffer.getObjectId(1))
    Assert.assertEquals(432, buffer.getAnalyticObjectId(1))

    Assert.assertEquals(345, DataTypeUtils.convertAsInt(buffer.apply(2)))
    Assert.assertEquals(543, buffer.getAnalyticObjectId(2))

    Assert.assertEquals(111, buffer.getInt(3))
    Assert.assertEquals(121, buffer.getObjectId(3))

    Assert.assertEquals(131, buffer.getInt(4))
    Assert.assertEquals(141, buffer.getObjectId(4))
    Assert.assertEquals(141, buffer.getAnalyticObjectId(4))
    buffer.close()
  }
}
