// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.jni.services

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

import monad.jni.services.gen.{LoggerLevel, CMonad}
import monad.support.services.LoggerSupport
import org.fusesource.hawtjni.runtime

/**
 * JNI Loader
 */
object JniLoader extends LoggerSupport {
  private final val MONAD_LIBRARY = new runtime.Library("monad4j", getClass)
  private final val atomicBoolean = new AtomicBoolean(false)

  def loadJniLibrary(serverHome: String, logFile: String) {
    if (atomicBoolean.compareAndSet(false, true)) {
      val file = new File(serverHome)
      val path = System.getProperty(MonadJniConstants.MONAD_JNI_LIBRARY_KEY)
      if (path == null)
        System.setProperty(MonadJniConstants.MONAD_JNI_LIBRARY_KEY, file.getAbsolutePath + "/dll")
      MONAD_LIBRARY.load()

      if (System.getProperty("enable-log") != "true" && logFile != "stderr") {
        CMonad.OpenLogger(logFile + ".internal", LoggerLevel.LOGGER_LEVEL_INFO);
      } else {
        CMonad.OpenLogger("stderr", LoggerLevel.LOGGER_LEVEL_DEBUG) //debug
      }
    }
  }
}
