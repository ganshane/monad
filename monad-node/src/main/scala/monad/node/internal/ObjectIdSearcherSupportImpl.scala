// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import javax.naming.SizeLimitExceededException

import monad.face.config.IndexConfigSupport
import monad.face.model.IdShardResult
import monad.node.internal.support.SearcherManagerSupport
import org.apache.lucene.index.{LeafReaderContext, SegmentReader}
import org.apache.lucene.search.{Scorer, SimpleCollector}
import org.apache.lucene.util.LongBitSet
import org.slf4j.LoggerFactory

/**
 * 对象搜索的
 * @author jcai
 */
abstract class ObjectIdSearcherSupportImpl(regionId: Short)
  extends SearcherManagerSupport
  with QueryParserSupport {
  private val logger = LoggerFactory getLogger getClass

  protected def getIndexConfig: IndexConfigSupport

  /**
   * 搜索对象
   * @param q 搜索条件
   * @return
   */
  def searchObjectId(q: String): IdShardResult = {

    val parser = createParser()
    val query = parser.parse(q)
    logger.info("object id query :{} ....", q)
    val start = System.currentTimeMillis()
    var originCollector: IdSearchCollector = null
    doInSearcher { s =>
      originCollector = new IdSearchCollector(s)
      try {
        val collector = new ResultLimitCollector(new TimeOutCollector(originCollector), getIndexConfig.index.queryMaxLimit)
        s.search(query, collector)
      } catch {
        case e: SizeLimitExceededException =>
          logger.warn("over size limit")
      }
    }
    //originCollector.result.optimize()
    val time = System.currentTimeMillis() - start
    val resultSize = originCollector.result.cardinality()
    logger.info("object id query :{},size:{} time:" + time, q, resultSize)
    if (resultSize > 0) {
      val idShardResult = new IdShardResult
      idShardResult.data = originCollector.result
      idShardResult.region = regionId
      return idShardResult
    }
    else
      return null
  }

  private class IdSearchCollector(s: InternalIndexSearcher) extends SimpleCollector {
    private var context: LeafReaderContext = _
    //TODO 优化采用 FIXEDBITSET
    private[internal] val result = new LongBitSet(102400)


    override def doSetNextReader(context: LeafReaderContext): Unit = {
      this.context = context;
    }


    override def setScorer(scorer: Scorer): Unit = super.setScorer(scorer)

    def collect(doc: Int) {
      val idSeq = readObjectId(doc)
      if (idSeq <= 0) return
      //logger.debug("doc:{} idseq:{}",doc,idSeq)
      result.set(idSeq)
    }

    private def readObjectId(doc: Int): Int = {
      s.analyticObjectId(this.context.reader().asInstanceOf[SegmentReader], doc)
    }

    override def needsScores(): Boolean = false
  }
}
