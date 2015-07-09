// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.ExecutorService

import monad.face.MonadFaceConstants
import monad.face.model.ResourceDefinition
import monad.node.internal.support.GlobalObjectIdCache
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException
import org.apache.lucene.index.{IndexReader, LeafReaderContext, SegmentReader}
import org.apache.lucene.search.IndexSearcher

/**
 * 针对某一资源进行搜索,增加了id缓存的支持
 * @author jcai
 */
class InternalIndexSearcher(reader: IndexReader, rd: ResourceDefinition, executor: ExecutorService)
  extends IndexSearcher(reader, executor)
  //with ObjectIdCacheSupport
  {
  //默认只有数据的Id，为整型
  private val payloadLength = 4
  /*
  //如果分析的话，加上数据的hash值
  if (findObjectIdColumn) {
    payloadLength = GlobalObjectIdCache.FULL_LENGTH
  }
  */

  /*
  for (readerContext <- leafContexts) {
    loadObjectIdWithLocalCache(rd.name, readerContext.reader().asInstanceOf[SegmentReader])
  }
  */

  def objectId(docId: Int): Int = {
    val subReaderContext= getSubReaderContext(docId)
    val docValues = subReaderContext.reader().getNumericDocValues(MonadFaceConstants.OBJECT_ID_PAYLOAD_FIELD)
    val objectId = docValues.get(docId-subReaderContext.docBaseInParent)
    objectId.asInstanceOf[Int]

    //getObjectIdCache(reader.reader().asInstanceOf[SegmentReader]).apply(docId - reader.docBaseInParent)
    /*
    if (payloadLength == GlobalObjectIdCache.FULL_LENGTH){
    }else{
        val reader = getSubReaderContext(docId)
        val l = reader.reader().asInstanceOf[SegmentReader].getNumericDocValues(MonadFaceConstants.OBJECT_ID_PAYLOAD_FIELD)
          .get(docId-reader.docBaseInParent)
        val objId = (l >>> 32).toInt
        DataTypeUtils.convertIntAsArray(objId)
    }
    */
  }

  private def getSubReaderContext(n: Int): LeafReaderContext = {
    // find reader for doc n:
    val size = leafContexts.size()
    1 until size foreach { i =>
      val context = leafContexts.get(i)
      if (context.docBaseInParent > n) {
        return leafContexts.get(i - 1)
      }
    }
    leafContexts.get(size - 1)
  }

  def analyticObjectId(reader: SegmentReader, docId: Int): Int = {
    if (payloadLength != GlobalObjectIdCache.FULL_LENGTH) throw new MonadException("object id must 8,", MonadNodeExceptionCode.INVALID_INDEX_PAYLOAD)
    //getObjectIdCache(reader).getAnalyticObjectId(docId)
    throw new UnsupportedOperationException("analytic object id not supported!")
  }

  //得到payload的字节长度
  protected def getPayloadBytesLength = payloadLength

  private def findObjectIdColumn: Boolean = {
    val it = rd.properties.iterator()
    while (it.hasNext) {
      if ((it.next().mark & 8) == 8) {
        return true
      }
    }
    false
  }
}

