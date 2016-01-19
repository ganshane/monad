// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import com.sun.jna.{Library, Native}

/**
 * 针对Linux的内存锁定操作
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
object MemoryLockerLinux {

  // http://www-numi.fnal.gov/offline_software/srt_public_context/WebDocs/Errors/unix_system_errors.html

  // #define EPERM 1 /* Operation not permitted */ The calling process does not have the appropriate privilege to perform the
  // requested operation.
  final val EPERM = 1;

  // #define EAGAIN 11 /* Try again */ Some or all of the memory identified by the operation could not be locked when the call was
  // made.
  final val EAGAIN = 11;

  // #define ENOMEM 12 /* Out of memory */ Locking all of the pages currently mapped into the address space of the process would
  // exceed an implementation-dependent limit on the amount of memory that the process may lock.
  final val ENOMEM = 12;

  // #define EINVAL 22 /* Invalid argument */ The flags argument is zero, or includes unimplemented flags.
  final val EINVAL = 22;

  // #define ENOSYS 38 /* Function not implemented */ The implementation does not support this memory locking interface.
  final val ENOSYS = 38;

  // Linux/include/asm-generic/mman.h
  //
  // 16 #define MCL_CURRENT 1 /* lock all current mappings */
  // 17 #define MCL_FUTURE 2 /* lock all future mappings */

  final val LOCK_CURRENT_MEMORY = 1;
  final val LOCK_ALL_MEMORY_DURING_APPLICATION_LIFE = 2;

  val INSTANCE: MemoryLockerLinux = Native.loadLibrary("c", classOf[MemoryLockerLinux]).asInstanceOf[MemoryLockerLinux]
}

trait MemoryLockerLinux extends Library {

  /**
   * This method locks all memory under *nix operating system using kernel function {@code mlockall}. details of this function you
   * can find on {@see http://www.kernel.org/doc/man-pages/online/pages/man2/mlock.2.html}
   *
   * @param flags determines lock memory on startup or during life of application.
   *
   * @return Upon successful completion, the mlockall() function returns a value of zero. Otherwise, no additional memory is locked,
   *         and the function returns a value of -1 and sets errno to indicate the error. The effect of failure of mlockall() on
   *         previously existing locks in the address space is unspecified. If it is supported by the implementation, the
   *         munlockall() function always returns a value of zero. Otherwise, the function returns a value of -1 and sets errno to
   *         indicate the error.
   */
  def mlockall(flags: Int): Int
}
