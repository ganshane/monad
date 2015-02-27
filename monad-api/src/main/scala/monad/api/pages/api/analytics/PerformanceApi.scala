// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
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
