package monad.node.internal

import org.junit.Test

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class MemoryLockerTest {
  @Test
  def test_lock {
    MemoryLocker.lockMemory()
  }
}
