// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.nio.ByteBuffer

/**
 * 针对数据进行转换
 */
object CodingHelper {
  def EncodeInt32WithBigEndian(i: Int): Array[Byte] = {
    ByteBuffer.allocate(4).putInt(i).array()
  }

  def DecodeInt32WithBigEndian(bytes: Array[Byte], offset: Int = 0): Int = {
    ByteBuffer.wrap(bytes).getInt
  }

  def DecodeInt32WithLittleEndian(bytes: Array[Byte], offset: Int = 0): Int = {
    var i = 0
    i |= bytes(offset) & 0xff
    i |= (bytes(offset + 1) << 8) & 0xffff
    i |= (bytes(offset + 2) << 16) & 0xffffff
    i |= (bytes(offset + 3) << 24) & 0xffffffff

    i
  }

  def convertAsBytes(i: Int): Array[Byte] = {
    val bytes = new Array[Byte](4)
    bytes(0) = (i >>> 24).asInstanceOf[Byte]
    bytes(1) = (i >>> 16).asInstanceOf[Byte]
    bytes(2) = (i >>> 8).asInstanceOf[Byte]
    bytes(3) = i.asInstanceOf[Byte]

    bytes
  }

  def convertAsInt(bytes: Array[Byte], offset: Int = 0): Int = {
    var i = 0
    i |= bytes(offset) << 24
    i |= bytes(offset + 1) << 16
    i |= bytes(offset + 2) << 8
    i |= bytes(offset + 3)

    i
  }
}
