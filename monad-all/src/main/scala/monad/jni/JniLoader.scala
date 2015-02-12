// Copyright 2013 The EGF Software Department.
// site: http://www.etgoldenfinger.com
package monad.jni

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

import org.fusesource.hawtjni.runtime


/**
 * jni loader
 * @author jcai
 */
object JniLoader {
  private final val LIBRARY = new runtime.Library("monad4j", getClass)
  private final val atomicBoolean = new AtomicBoolean(false)

  def loadJniLibrary(serverHome: String) {
    if (atomicBoolean.compareAndSet(false, true)) {
      val file = new File(serverHome)
      System.setProperty("library.monad4j.path", file.getAbsolutePath + "/dll")
      LIBRARY.load()
    }
  }
}
