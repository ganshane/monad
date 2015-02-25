package monad.api.internal

import java.io.ByteArrayOutputStream

import monad.face.model.OpenBitSetWithNodes
import org.apache.lucene.util.OpenBitSet
import org.apache.tapestry5.services.Response
import org.junit.Test
import org.mockito.Mockito

/**
 *
 * @author jcai
 */
class OpenBitSetResultProcessorTest {
  @Test
  def test_process() {
    val response = Mockito.mock(classOf[Response])
    val os = new ByteArrayOutputStream()
    Mockito.when(response.getOutputStream("application/octet-stream")).thenReturn(os)
    val processor = new OpenBitSetResultProcessor(response)

    val bitSet = new OpenBitSet()
    bitSet.set(1)
    bitSet.set(1 << 9)
    /*
    1 until 100 foreach{i=>
        bitSet.set(i+ (1 << 8))
    }
    */
    processor.processResultValue(new OpenBitSetWithNodes(bitSet))

    val byteArray = os.toByteArray
    //Assert.assertEquals(100 * 4,byteArray.length)
  }
}
