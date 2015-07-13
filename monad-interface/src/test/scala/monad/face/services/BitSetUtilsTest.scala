package monad.face.services

import monad.face.internal.MonadSparseFixedBitSet
import org.junit.{Assert, Test}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-14
 */
class BitSetUtilsTest {
  @Test
  def test_bitset: Unit ={
    val bitSet = new MonadSparseFixedBitSet(1000)
    bitSet.set(888)
    bitSet.set(188)
    bitSet.set(288)
    val bb = BitSetUtils.serialize(bitSet)
    val bitSet2 = BitSetUtils.deserialize(bb)
    Assert.assertTrue(bitSet2.get(888))
    Assert.assertTrue(bitSet2.get(188))
    Assert.assertTrue(bitSet2.get(288))
    Assert.assertFalse(bitSet2.get(388))
  }
}
