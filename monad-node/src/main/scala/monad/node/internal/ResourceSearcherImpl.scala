// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.Date
import java.util.concurrent.ExecutorService
import javax.naming.SizeLimitExceededException

import monad.face.config.IndexConfigSupport
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.model.{ColumnType, ResourceDefinition, ShardResult}
import monad.face.services.ResourceSearcher
import monad.node.internal.support.SearcherManagerSupport
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException
import org.apache.lucene.index.{IndexReader, IndexWriter, LeafReaderContext}
import org.apache.lucene.search._
import org.apache.lucene.util.LongBitSet
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.control.NonFatal


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
          case NonFatal(e) =>
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
      val it = rd.properties.iterator()
      val sortOpt = sortStr.trim.split("\\s+").toList match{
        case field::"asc"::Nil =>
          createSortField(field,false,it)
        case field::Nil =>
          createSortField(field,false,it)
        case field::"desc"::Nil =>
          createSortField(field,true,it)
        case o =>
          None
      }
      sortOpt foreach(x=>sort=x)
    }
    /*
    else {
        //如果没有设置sort，则使用相关性来进行查询
        //sort = new Sort(SortField.FIELD_DOC)
    }
    */
    var collector: TopDocsCollector[_] = null
    if (sort == null) {
      collector = TopScoreDocCollector.create(topN)
    } else {
      collector = TopFieldCollector.create(sort, topN, false, false, false)
    }

    val startTime = new Date().getTime
    var filter: Filter = null
    if (config.index.queryMaxLimit > 0) {
      filter = new SizeLimitedFilter(config.index.queryMaxLimit)
    }
    doInSearcher { searcher =>
      /*
      val booleanQuery = new BooleanQuery()
      booleanQuery.add(query,BooleanClause.Occur.MUST)
      booleanQuery.add(filter,BooleanClause.Occur.FILTER)
      val topDocs = searcher.search(booleanQuery, topN)
      */
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
      case NonFatal(e) =>
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
      collector = TopScoreDocCollector.create(topN)
    } else {
      collector = TopFieldCollector.create(sort, topN, false, false, false)
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
      collector = TopScoreDocCollector.create(topN)
    } else {
      collector = TopFieldCollector.create(sort, topN, false, false, false)
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

  @tailrec
  private def createSortField(sort:String,reverse:Boolean,it:java.util.Iterator[ResourceProperty]): Option[Sort] ={
    if(it.hasNext){
      val property = it.next()
      if(property.name == sort){
        property.columnType match{
          case ColumnType.Long =>
            Some(new Sort(new SortField(sort,SortField.Type.LONG,reverse)))
          case ColumnType.Int | ColumnType.Date =>
            Some(new Sort(new SortField(sort,SortField.Type.INT,reverse)))
          case ColumnType.String | ColumnType.Clob =>
            Some(new Sort(new SortField(sort,SortField.Type.STRING,reverse)))
          case other=>
            logger.error("wrong column type:{}",other)
            None
        }
      }else{
        createSortField(sort,reverse,it)
      }
    }else{
      None
    }
  }

  private class NormalSearcherCollector(topN: Int) extends SimpleCollector {
    val result = new LongBitSet(1000)
    var totalHits = 0
    private var docBase = 0


    override def doSetNextReader(context: LeafReaderContext): Unit = {
      docBase = context.docBase
    }


    def collect(doc: Int) {
      if (totalHits < topN)
        result.set(doc + docBase)
      totalHits += 1
    }

    override def needsScores(): Boolean = true
  }

  private class LimitSearcherCollector(collector: Collector) extends SimpleCollector {
    private final val limit = 5000000
    var totalHits = 0
    private var notReachMax = true
    private var context: LeafReaderContext = _


    override def setScorer(scorer: Scorer): Unit = {
      collector.getLeafCollector(context).setScorer(scorer)
    }


    override def doSetNextReader(context: LeafReaderContext): Unit = {
      this.context = context
    }

    def collect(doc: Int) {
      if (notReachMax) {
        collector.getLeafCollector(context).collect(doc)
        notReachMax = limit > totalHits
        if (!notReachMax) {
          throw new SizeLimitExceededException()
        }
      }
      totalHits += 1
    }

    override def needsScores(): Boolean = collector.needsScores()
  }
}
