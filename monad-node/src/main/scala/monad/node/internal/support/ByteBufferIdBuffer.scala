// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal.support

import java.nio.ByteBuffer
import java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}

import monad.face.services.DataTypeUtils
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException

/**
 * 基于byte buffer的Id缓存类
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class ByteBufferIdBuffer(maxDoc: Int, blockSize: Int, hackSupported: Boolean) extends IdBuffer {
  private val size = maxDoc * blockSize
  private val buffer = ByteBuffer.allocateDirect(size)

  def put(bytes: Array[Byte]) {
    buffer.put(bytes)
  }

  //读取前四位的id
  def apply(docId: Int) = {
    val arr = new Array[Byte](4)
    val start = docId * blockSize
    0 until 4 foreach { i =>
      arr(i) = buffer.get(start + i)
    }
    arr
  }

  //从第四位开始读取分析对象的ID
  def getAnalyticObjectId(docId: Int) = {
    val arr = new Array[Byte](4)
    val start = docId * blockSize
    0 until 4 foreach { i =>
      try {
        arr(i) = buffer.get(start + i + 4)
      } catch {
        case e: IndexOutOfBoundsException =>
          throw new MonadException("docid:%s,bufferId:%s".format(docId, start + i + 4), MonadNodeExceptionCode.OVERFLOW_DIRECT_BUFFER)
      }
    }
    DataTypeUtils.convertAsInt(arr)
  }

  def close() {
    if (hackSupported) {
      try {
        AccessController.doPrivileged(new PrivilegedExceptionAction[Object]() {
          def run(): Object = {
            val getCleanerMethod = buffer.getClass.getMethod("cleaner")
            getCleanerMethod.setAccessible(true)
            val cleaner = getCleanerMethod.invoke(buffer)
            if (cleaner != null) {
              cleaner.getClass.getMethod("clean").invoke(cleaner)
            }
            null
          }
        })
      } catch {
        case e: PrivilegedActionException =>
          throw MonadException.wrap(e, MonadNodeExceptionCode.UNABLE_UNMAP_BUFFER)
      }
    }
  }
}
