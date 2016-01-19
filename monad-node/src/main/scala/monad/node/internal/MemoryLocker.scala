// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import com.sun.jna.Native
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException
import org.slf4j.LoggerFactory

/**
 * memory locker
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
object MemoryLocker {
  private val logger = LoggerFactory getLogger getClass

  /**
   * This method locks memory to prevent swapping. This method provide information about success or problems with locking memory.
   * You can reed console output to know if memory locked successfully or not. If system error occurred such as permission any
   * specific exception will be thrown.
   */
  def lockMemory() {
    try {
      val errorCode = MemoryLockerLinux.INSTANCE.mlockall(MemoryLockerLinux.LOCK_CURRENT_MEMORY)
      if (errorCode != 0) {

        var errorMessage: String = null
        val lastError = Native.getLastError
        lastError match {
          case MemoryLockerLinux.EPERM =>
            errorMessage = "The calling process does not have the appropriate privilege to perform the requested operation(EPERM).";
          case MemoryLockerLinux.EAGAIN =>
            errorMessage = "Some or all of the memory identified by the operation could not be locked when the call was made(EAGAIN).";
          case MemoryLockerLinux.ENOMEM =>
            errorMessage = "Unable to lock JVM memory. This can result in part of the JVM being swapped out, especially if mmapping of files enabled. Increase RLIMIT_MEMLOCK or run monad server as root(ENOMEM).";
          case MemoryLockerLinux.EINVAL =>
            errorMessage = "The flags argument is zero, or includes unimplemented flags(EINVAL).";
          case MemoryLockerLinux.ENOSYS =>
            errorMessage = "The implementation does not support this memory locking interface(ENOSYS).";
          case other =>
            errorMessage = "Unexpected exception with code " + lastError + "."
        }
        throw new MonadException("Error occurred while locking memory: " + errorMessage, MonadNodeExceptionCode.FAIL_TO_LOCK_MEMORY)

      } else {
        logger.info("Memory locked successfully!")
      }

    } catch {
      case e: UnsatisfiedLinkError =>
        throw new MonadException("Cannot lock virtual memory. " +
          "It seems that you OS (" + System.getProperty("os.name") + ") doesn't support ",
          MonadNodeExceptionCode.FAIL_TO_LOCK_MEMORY)
    }
  }
}
