package monad.core.internal

import java.nio.ByteBuffer

import monad.face.services.DataTypeUtils
import org.junit.{Assert, Test}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class ObjectIdCreatorImplTest {
  @Test
  def test_objectId {
    val creator = new ObjectIdCreatorImpl
    val x = DataTypeUtils.convertIntAsArray(123)
    val serverHash: Short = 12
    val bytes = ByteBuffer.allocate(6).putShort(serverHash).put(x).array()
    val str = creator.objectIdToString(bytes)
    println(str);
    val r = creator.stringToObjectId(str)
    Assert.assertArrayEquals(bytes, r)
  }
}
