package monad.node.internal

import javax.naming.SizeLimitExceededException
import monad.face.config.IndexConfigSupport
import monad.face.model.IdShardResult
import monad.node.internal.support.SearcherManagerSupport
import org.apache.lucene.index.{AtomicReaderContext, SegmentReader}
import org.apache.lucene.search.{Collector, Scorer}
import org.apache.lucene.util.OpenBitSet
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

  private class IdSearchCollector(s: InternalIndexSearcher) extends Collector {
    private var context: AtomicReaderContext = _
    private[internal] val result = new OpenBitSet(102400)

    def setScorer(scorer: Scorer) {}

    def collect(doc: Int) {
      val idSeq = readObjectId(doc)
      if (idSeq <= 0) return
      //logger.debug("doc:{} idseq:{}",doc,idSeq)

      result.set(idSeq)
    }


    def setNextReader(context: AtomicReaderContext) {
      this.context = context
    }

    private def readObjectId(doc: Int): Int = {
      s.analyticObjectId(this.context.reader().asInstanceOf[SegmentReader], doc)
    }

    def acceptsDocsOutOfOrder() = true
  }

}
