// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal.support

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import monad.face.MonadFaceConstants
import monad.face.services.DataTypeUtils
import org.apache.lucene.index.LeafReader.CoreClosedListener
import org.apache.lucene.index.SegmentReader
import org.apache.lucene.util.Bits
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

private[monad] object GlobalObjectIdCache {
  final val FULL_LENGTH = 8
  //全局缓存对象
  final val cache = new ConcurrentHashMap[Any, IdBuffer]()
  val logger = LoggerFactory getLogger getClass
  //全局locker
  val locker = new ReentrantLock()
  val corePurgeListener = new CoreClosedListener {

    def onClose(ownerCoreCacheKey: Any) {
      try {
        locker.lock()
        val buffer = cache.remove(ownerCoreCacheKey)
        if (buffer != null) {
          //释放buffer
          buffer.close()
        }
      } finally {
        locker.unlock()
      }
    }
  }
  /**
   * <code>true</code>, if this platform supports unmapping mmapped files.
   */
  var UNMAP_SUPPORTED: Boolean = false
  try {
    Class.forName("sun.misc.Cleaner")
    Class.forName("java.nio.DirectByteBuffer")
      .getMethod("cleaner")
    UNMAP_SUPPORTED = true
  } catch {
    case NonFatal(e) =>
  }
}

/**
 * object id 缓存
 * @author jcai
 */
trait ObjectIdCacheSupport {

  import monad.node.internal.support.GlobalObjectIdCache._

  final val DELETED_UID = DataTypeUtils.convertIntAsArray(-1)
  final val DELETED_UID_WITH_ANALYSIS_ID = ByteBuffer.allocate(FULL_LENGTH).putInt(-1).putInt(-1).array()

  def getObjectIdCache(reader: SegmentReader) = {
    cache.get(reader.getCoreCacheKey)
  }

  def loadObjectIdWithLocalCache(name: String, reader: SegmentReader) {
    var objectIds: IdBuffer = null
    val key = reader.getCoreCacheKey
    objectIds = cache.get(key)
    if (objectIds == null) {
      try {
        locker.lock()
        objectIds = cache.get(key)
        if (objectIds == null) {
          logger.debug("building object id cache {} for key:{}", reader.getSegmentName, key)
          objectIds = readObjectIdAsArray(name, reader)
          cache.put(key, objectIds)
          reader.addCoreClosedListener(corePurgeListener)
        }
      } finally {
        locker.unlock()
      }
    } else {
      if (logger.isDebugEnabled) {
        logger.debug("load object id from cache:{}", reader.getCoreCacheKey)
      }
    }
  }

  //直接存入对象Int避免过多的转换
  private def readObjectIdAsArray(name: String, reader: SegmentReader): IdBuffer = {
    val docValues = reader.getNumericDocValues(MonadFaceConstants.OBJECT_ID_PAYLOAD_FIELD)
    val maxDoc = reader.maxDoc()
    val buffer = new JNAIdBuffer(reader.maxDoc(), getPayloadBytesLength)
    val liveDocs: Bits = reader.getLiveDocs
    for (i <- 0 until maxDoc) {
      if (liveDocs != null && !liveDocs.get(i)) {
        buffer.put(MonadFaceConstants.DELETED_SID)
      } else {
        buffer.put(docValues.get(i))
      }
    }

    buffer
  }

  //得到payload的字节长度
  protected def getPayloadBytesLength = 4
}
