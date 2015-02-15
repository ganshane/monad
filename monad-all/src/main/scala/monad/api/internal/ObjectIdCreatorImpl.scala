// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.internal

import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import monad.api.services.{ObjectIdCreator, MonadApiExceptionCode}
import monad.support.services.MonadException
import org.slf4j.LoggerFactory

/**
 * NoSQL全局的对象id创建器
 * @author jcai
 */
class ObjectIdCreatorImpl extends ObjectIdCreator {
  private lazy val _genmachine: Int = generateMachineId
  private val logger = LoggerFactory getLogger getClass
  private val _nextInc = new AtomicInteger((new java.util.Random()).nextInt())

  def createObjectId(resourceName: String): Array[Byte] = {
    val _time = (System.currentTimeMillis() / 1000).asInstanceOf[Int]
    val _machine = _genmachine
    val _inc = _nextInc.getAndIncrement

    val b = new Array[Byte](12)
    val bb = ByteBuffer.wrap(b)
    // by default BB is big endian like we need
    bb.putInt(_time)
    bb.putInt(_machine)
    bb.putInt(_inc)

    b
  }

  def objectIdToString(b: Array[Byte]): String = {
    val buf = new StringBuilder(12)
    0 until b.length foreach { i =>
      val x = b(i) & 0xFF
      val s = Integer.toHexString(x)
      if (s.length() == 1)
        buf.append("0")
      buf.append(s)
    }
    buf.toString()
  }


  def stringToObjectId(s: String) = {
    if (s.length != 12) {
      throw new MonadException("非法的id数据" + s, MonadApiExceptionCode.INVALIDATE_OBJECT_ID)
    }
    val b = new Array[Byte](6)
    0 until b.length foreach { i =>
      b(i) = Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16).asInstanceOf[Byte]
    }
    b
  }

  private def generateMachineId = {
    // build a 2-byte machine piece based on NICs info
    var machinePiece: Int = 0;
    {
      val sb = new StringBuilder()
      val e = NetworkInterface.getNetworkInterfaces
      while (e.hasMoreElements) {
        val ni = e.nextElement()
        sb.append(ni.toString)
      }
      machinePiece = sb.toString().hashCode() << 16
      logger.debug("machine piece post: " + Integer.toHexString(machinePiece))
    }

    // add a 2 byte process piece. It must represent not only the JVM but the class loader.
    // Since static var belong to class loader there could be collisions otherwise
    var processPiece = 0;
    {
      var processId = new java.util.Random().nextInt()
      try {
        processId = java.lang.management.ManagementFactory.getRuntimeMXBean.getName.hashCode()
      }
      catch {
        case e: Throwable =>
      }

      val loader = getClass.getClassLoader
      val loaderId = if (loader != null) System.identityHashCode(loader) else 0

      val sb = new StringBuilder()
      sb.append(Integer.toHexString(processId))
      sb.append(Integer.toHexString(loaderId))
      processPiece = sb.toString().hashCode() & 0xFFFF
      logger.debug("process piece: " + Integer.toHexString(processPiece))
    }

    machinePiece | processPiece
  }
}
