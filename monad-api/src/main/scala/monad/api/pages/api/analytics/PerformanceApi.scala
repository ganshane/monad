package monad.api.pages.api.analytics

import org.apache.lucene.util.OpenBitSet

/**
 *
 * @author jcai
 */
class PerformanceApi {
  def onActivate() = {
    val bitSet = new OpenBitSet();
    1 until 10000000 foreach { i =>
      bitSet.set(i)
    }
    bitSet
  }
}
