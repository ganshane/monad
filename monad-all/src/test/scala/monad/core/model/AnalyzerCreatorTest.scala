// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.model

import monad.face.model.AnalyzerType
import org.junit.{Assert, Test}


/**
 *
 * @author jcai
 */

class AnalyzerCreatorTest {
  @Test
  def test_create() {
    val analyzer = AnalyzerCreator.create(AnalyzerType.Standard)
    Assert.assertNotNull(analyzer)
  }
}
