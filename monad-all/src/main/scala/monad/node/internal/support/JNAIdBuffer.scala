package monad.node.internal.support

import java.nio.ByteBuffer

import com.sun.jna.{Native, Pointer}
import monad.face.services.DataTypeUtils
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException

/**
 * 基于JNA的IdBuffer
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class JNAIdBuffer(maxDoc: Int, blockSize: Int) extends IdBuffer {
  private val size = maxDoc * blockSize
  private val pointerValue = Native.malloc(size)
  private val pointer = new Pointer(pointerValue)
  if (pointerValue == 0) {
    throw new MonadException(MonadNodeExceptionCode.FAIL_TO_ALLOCATE_MEMORY)
  }
  private var writeOffset = 0

  /**
   * 设置数据
   * @param bytes
   */
  def put(bytes: Array[Byte]) {
    val buffer = ByteBuffer.wrap(bytes)
    put(buffer.getInt(0)) // __id
    if (bytes.length == GlobalObjectIdCache.FULL_LENGTH) {
      put(buffer.getInt(4)) //__objId
    }
    //pointer.write(writeOffset,bytes,0,bytes.length)
    //writeOffset += bytes.length
  }

  def put(i: Int) {
    pointer.setInt(writeOffset, i)
    writeOffset += 4
  }

  def put(i: Long) {
    if (blockSize == GlobalObjectIdCache.FULL_LENGTH) {
      pointer.setLong(writeOffset, i)
      writeOffset += 8
    } else {
      put(i.toInt)
    }
    /*
    put((i>>>32).toInt)
    put((i&0xfffffffL).intValue())
    */
  }

  /**
   * 读取某一文档对应的NoSQL的ID
   * @param docId 文档ID
   * @return NoSQL的ID值
   */
  def apply(docId: Int) = {
    val start = docId * blockSize
    DataTypeUtils.convertIntAsArray(pointer.getInt(start))
  }

  def getInt(docId: Int) = {
    val start = docId * blockSize
    pointer.getInt(start)
  }

  /**
   * 得到分析对象的文档ID
   * @param docId 文档ID
   * @return 分析对象的值
   */
  def getAnalyticObjectId(docId: Int) = {
    getObjectId(docId)
  }

  def getObjectId(docId: Int) = {
    val start = docId * blockSize + 4
    pointer.getInt(start)
  }

  /**
   * 关闭这个buffer
   */
  def close() {
    Native.free(pointerValue)
  }
}
