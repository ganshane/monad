package monad.jni

import java.nio.ByteBuffer

import monad.face.services.DataTypeUtils

/**
 * syncbinlog的解析
 * //SEQ+KV+COMMAND+Key
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class NoSQLBinlogValue(bytes: Array[Byte]) {
  def seq(): Long = {
    ByteBuffer.wrap(bytes, 0, 8).getLong
  }

  def dataType(): DataType = {
    DataType.swigToEnum(bytes(8))
  }

  def commandType(): DataCommandType = {
    DataCommandType.swigToEnum(bytes(9))
  }

  def keyBytes(): Array[Byte] = {
    val bytesLength = 4
    val r = new Array[Byte](bytesLength)
    System.arraycopy(bytes, 10, r, 0, bytesLength)

    r
  }

  def keyInt() = DataTypeUtils.convertAsInt(bytes, 10)

  def objectId(): Option[Int] = {
    if (bytes.length == 18) {
      /*
      val r = new Array[Byte](4)
      System.arraycopy(bytes,14,r,0,4)
      */
      return Some(ByteBuffer.wrap(bytes, 14, 4).getInt)
    }
    None
  }
}
