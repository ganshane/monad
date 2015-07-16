package monad.face.services

import java.io.ByteArrayInputStream

import monad.face.internal.MonadSparseFixedBitSet
import org.apache.lucene.util.{BitSetIterator, SparseFixedBitSet}
import org.junit.{Assert, Test}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-14
 */
class BitSetUtilsTest {
  @Test
  def test_bitset1: Unit = {
    val bitSet = new SparseFixedBitSet(10000)
    bitSet.set(1000)
    bitSet.set(3000)
    bitSet.set(5000)
    val bitSet2 = new SparseFixedBitSet(10000)
    bitSet2.set(3000)
    val bitSetIt =new BitSetIterator(bitSet2,1)

    bitSet.and(bitSetIt);

    Assert.assertTrue(bitSet.get(3000));
    Assert.assertFalse(bitSet.get(1000));

    //Assert.assertEquals(9000,bitSet.nextSetBit(8001))
  }
  @Test
  def test_bitset: Unit ={
    val bitSet = new MonadSparseFixedBitSet(1000)
    bitSet.set(188)
    bitSet.set(288)
    bitSet.set(888)
    val bb = BitSetUtils.serialize(bitSet)
    val is = new ByteArrayInputStream(bb.array())
    val bitSet2 = BitSetUtils.deserialize(is)
    Assert.assertTrue(bitSet2.get(888))
    Assert.assertTrue(bitSet2.get(188))
    Assert.assertTrue(bitSet2.get(288))
    Assert.assertFalse(bitSet2.get(388))
  }
  @Test
  def test_bitset2: Unit ={
    val bitSet = new MonadSparseFixedBitSet(10000)
    0 until 10000 foreach{i=>
      bitSet.set(i);
    }
    val bb = BitSetUtils.serialize(bitSet)
    val is = new ByteArrayInputStream(bb.array())

    val bitSet2 = BitSetUtils.deserialize(is)
    Assert.assertTrue(bitSet2.get(888))
    Assert.assertTrue(bitSet2.get(188))
    Assert.assertTrue(bitSet2.get(288))
    Assert.assertTrue(bitSet2.get(388))
  }
}
