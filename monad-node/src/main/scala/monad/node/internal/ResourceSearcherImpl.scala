// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.Date
import java.util.concurrent.ExecutorService
import javax.naming.SizeLimitExceededException

import monad.face.config.IndexConfigSupport
import monad.face.model.{ResourceDefinition, ShardResult}
import monad.face.services.ResourceSearcher
import monad.node.internal.support.SearcherManagerSupport
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException
import org.apache.lucene.index.{AtomicReaderContext, IndexReader, IndexWriter}
import org.apache.lucene.search._
import org.apache.lucene.util.OpenBitSet
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.slf4j.LoggerFactory


/**
 * 搜索的实现类
 * @author jcai
 */
class ResourceSearcherImpl(val rd: ResourceDefinition, writer: IndexWriter, val regionId: Short, executor: ExecutorService)
  extends ObjectIdSearcherSupportImpl(regionId)
  with SearcherManagerSupport
  with QueryParserSupport
  with ResourceSearcher {
  private final val warmQuery = "抢劫 网吧 旅馆"
  protected val logger = LoggerFactory getLogger getClass
  private var searcherManager: SearcherManager = null
  private[internal] var config: IndexConfigSupport = null

  override def facetSearch(q: String, field: String, upper: Int, lower: Int): ShardResult = {
    throw new UnsupportedOperationException
  }

  /**
   * 启动对象实例
   */
  def start() {
    initQueryParser(rd)
    searcherManager = new SearcherManager(writer, false, new SearcherFactory() {
      override def newSearcher(reader: IndexReader) = {
        val searcher = new InternalIndexSearcher(reader, rd, executor)
        try {
          warm(searcher)
        } catch {
          case e: Throwable =>
            logger.warn("Fail to warm index", e)
        }
        searcher
      }
    })
  }

  private def warm(searcher: IndexSearcher) {
    val parser = createParser()
    val query = parser.parse(warmQuery)
    searcher.search(query, 10)
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    InternalUtils.close(searcherManager)
  }

  def collectSearch(query: String, sort: String, topN: Int) = {
    //TODO 增加version
    val sb = new java.lang.StringBuilder(rd.name)
    sb.append(query)
    sb.append(sort)
    sb.append(topN)

    val result = SearchResultCache.getOrPut[ShardResult](sb.toString) {
      search(query, sort, topN)
    }
    result.maxDoc = maxDoc
    result.serverHash = regionId

    result
  }

  /**
   * search index with index name and keyword
   */
  private def search(q: String, sortStr: String, topN: Int): ShardResult = {
    logger.info("[{}] \"{}\"searching .... ", rd.name, q)
    val query = parseQuery(q)

    //sort
    var sort: Sort = null
    if (!InternalUtils.isBlank(sortStr)) {
      sort = new Sort(new SortField(sortStr, SortField.Type.STRING, true))
    }
    /*
    else {
        //如果没有设置sort，则使用相关性来进行查询
        //sort = new Sort(SortField.FIELD_DOC)
    }
    */
    var collector: TopDocsCollector[_] = null
    if (sort == null) {
      collector = TopScoreDocCollector.create(topN, false)
    } else {
      collector = TopFieldCollector.create(sort, topN, false, false, false, false)
    }

    val startTime = new Date().getTime
    var filter: Filter = null
    if (config.index.queryMaxLimit > 0) {
      filter = new SizeLimitedFilter(config.index.queryMaxLimit)
    }
    doInSearcher { searcher =>
      val topDocs = searcher.search(query, filter, topN)
      val endTime = new Date().getTime
      logger.info("[{}] q:{},time:{}ms,hits:{}",
        Array[Object](rd.name, q,
          (endTime - startTime).asInstanceOf[Object],
          topDocs.totalHits.asInstanceOf[Object]))
      val shardResult = new ShardResult
      shardResult.totalRecord = topDocs.totalHits
      shardResult.results = topDocs.scoreDocs.map(x => (searcher.objectId(x.doc), x.score))
      shardResult.serverHash = regionId
      shardResult.maxDoc = searcher.getIndexReader.numDocs()
      shardResult
    }
  }

  protected def parseQuery(q: String) = {
    val parser = createParser()
    try {
      parser.parse(q)
    } catch {
      case e: Throwable =>
        logger.error(e.toString)
        throw new MonadException("fail to parse:[" + q + "]", MonadNodeExceptionCode.FAIL_TO_PARSE_QUERY)
    }
  }

  def maxDoc: Int = doInSearcher(_.getIndexReader.numDocs())

  def collectSearch2(query: String, sort: String, topN: Int) = {
    search2(query, sort, topN)
  }

  /**
   * search index with index name and keyword
   */
  private def search2(q: String, sortStr: String, topN: Int): ShardResult = {
    logger.info("[{}] \"{}\"searching .... ", rd.name, q)
    val query = parseQuery(q)
    //sort
    var sort: Sort = null
    if (!InternalUtils.isBlank(sortStr)) {
      sort = new Sort(new SortField(sortStr, SortField.Type.STRING, true))
    }
    /*
    else {
        //如果没有设置sort，则使用相关性来进行查询
        //sort = new Sort(SortField.FIELD_DOC)
    }
    */
    var collector: TopDocsCollector[_] = null
    if (sort == null) {
      collector = TopScoreDocCollector.create(topN, false)
    } else {
      collector = TopFieldCollector.create(sort, topN, false, false, false, false)
    }

    val startTime = new Date().getTime
    doInSearcher { searcher =>
      val topDocs = searcher.search(query, new SizeLimitedFilter(5000000), topN); //,delegateCollector)

      val endTime = new Date().getTime
      logger.info("[{}] q:{},time:{}ms,hits:{}",
        Array[Object](rd.name, q,
          (endTime - startTime).asInstanceOf[Object],
          topDocs.totalHits.asInstanceOf[Object]))
      val shardResult = new ShardResult
      shardResult.totalRecord = topDocs.totalHits
      shardResult.results = topDocs.scoreDocs.map(x => (searcher.objectId(x.doc), x.score))
      shardResult.serverHash = regionId
      shardResult.maxDoc = searcher.getIndexReader.maxDoc()

      shardResult
    }
  }

  def maybeRefresh() {
    searcherManager.maybeRefresh()
  }

  protected def getIndexConfig = config;

  //全局搜索对象
  protected def getSearcherManager = searcherManager

  /**
   * search index with index name and keyword
   */
  private def search3(q: String, sortStr: String, topN: Int): ShardResult = {
    logger.info("[{}] \"{}\"searching .... ", rd.name, q)
    val query = parseQuery(q)
    //sort
    var sort: Sort = null
    if (!InternalUtils.isBlank(sortStr)) {
      sort = new Sort(new SortField(sortStr, SortField.Type.STRING, true))
    }
    /*
    else {
        //如果没有设置sort，则使用相关性来进行查询
        //sort = new Sort(SortField.FIELD_DOC)
    }
    */
    var collector: TopDocsCollector[_] = null
    if (sort == null) {
      collector = TopScoreDocCollector.create(topN, false)
    } else {
      collector = TopFieldCollector.create(sort, topN, false, false, false, false)
    }

    val startTime = new Date().getTime
    doInSearcher { searcher =>
      val delegateCollector = new LimitSearcherCollector(new TimeOutCollector(collector))
      try {
        searcher.search(query, delegateCollector)
      } catch {
        case e: SizeLimitExceededException =>
          logger.warn("[{}] over 5M", rd.name)
      }
      val topDocs = collector.topDocs(0, topN)

      val endTime = new Date().getTime
      logger.info("[{}] q:{},time:{}ms,hits:{}",
        Array[Object](rd.name, q,
          (endTime - startTime).asInstanceOf[Object],
          delegateCollector.totalHits.asInstanceOf[Object]))
      val shardResult = new ShardResult
      shardResult.totalRecord = delegateCollector.totalHits
      shardResult.results = topDocs.scoreDocs.map(x => (searcher.objectId(x.doc), x.score))
      shardResult.serverHash = regionId
      shardResult.maxDoc = searcher.getIndexReader.maxDoc()

      shardResult
    }
  }

  private class NormalSearcherCollector(topN: Int) extends Collector {
    val result = new OpenBitSet(1000)
    var totalHits = 0
    private var docBase = 0

    def setScorer(scorer: Scorer) {}

    def collect(doc: Int) {
      if (totalHits < topN)
        result.set(doc + docBase)
      totalHits += 1
    }


    def setNextReader(context: AtomicReaderContext) {
      this.docBase = context.docBase
    }

    def acceptsDocsOutOfOrder() = true
  }

  private class LimitSearcherCollector(collector: Collector) extends Collector {
    private final val limit = 5000000
    var totalHits = 0
    private var notReachMax = true

    def setScorer(scorer: Scorer) {
      if (notReachMax)
        collector.setScorer(scorer)
    }

    def collect(doc: Int) {
      if (notReachMax) {
        collector.collect(doc)
        notReachMax = limit > totalHits
        if (!notReachMax) {
          throw new SizeLimitExceededException()
        }
      }
      totalHits += 1
    }

    def setNextReader(context: AtomicReaderContext) {
      if (notReachMax)
        collector.setNextReader(context)
    }

    def acceptsDocsOutOfOrder() = collector.acceptsDocsOutOfOrder()
  }

}
