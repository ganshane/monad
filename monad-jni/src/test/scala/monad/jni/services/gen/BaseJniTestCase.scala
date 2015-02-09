package monad.jni.services.gen

import java.io.File

import monad.jni.services.JniLoader
import org.junit.Before

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-09
 */
class BaseJniTestCase {
  @Before
  def loadJni() {
    val file = new File("support")
    if (file.exists())
      JniLoader.loadJniLibrary("support", "stderr")
    else
      JniLoader.loadJniLibrary("../support", "stderr")
  }
}
