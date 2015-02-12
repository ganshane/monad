// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.api.internal

import java.util

import monad.api.model.SearchResult
import monad.api.services.SearcherQueue
import monad.core.model.AnalyzerCreator
import monad.face.MonadFaceConstants
import monad.face.model.{ResourceDefinition, ShardResult, ShardResultCollect}
import monad.face.services.RpcSearcherFacade
import monad.support.services.ServiceLifecycle
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.highlight.{Highlighter, QueryTermScorer, SimpleHTMLFormatter}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 *
 * @author jcai
 */
class SearcherQueueImpl(rd: ResourceDefinition, resourceSearcher: RpcSearcherFacade)
  extends ServiceLifecycle
  with SearcherQueue {
  private lazy val analyzerObj = AnalyzerCreator.create(rd.search.analyzer)
  private val logger = LoggerFactory.getLogger(classOf[SearcherQueueImpl])
  private val keyword = new KeywordAnalyzer()
  private val fieldAnalyzers = new util.HashMap[String, Analyzer]()
  private val analyzer: PerFieldAnalyzerWrapper = new PerFieldAnalyzerWrapper(this.analyzerObj, fieldAnalyzers)
  rd.properties.foreach(col => {
    //if (!col.primaryKey) {
    defaultSearchFields = defaultSearchFields :+ col.name
    //}
    if (col.isKeyword) {
      //针对keyword
      fieldAnalyzers.put(col.name, keyword)
      //analyzer.addAnalyzer(col.name,keyword)
    }
  })
  private var defaultSearchFields = Array[String]()

  def facetSearch(q: String, field: String, upper: Int, lower: Int): SearchResult = {
    val searchResults = resourceSearcher.facetSearch(rd.name, q, field, upper, lower)
    internalFacetSearch(searchResults, field)
  }

  private def internalFacetSearch(searchResults: ShardResult, field: String): SearchResult = {
    var results: Array[ShardResult] = null
    var result: SearchResult = null
    if (searchResults.isInstanceOf[ShardResultCollect]) {
      //集群查询情况下读取所有的值
      val shardResults = searchResults.asInstanceOf[ShardResultCollect]
      results = shardResults.shardResults
      result = SearchResult.mergeFacet(field, results.filterNot(_ == null))
      result.nodeAll = shardResults.nodesAll
      result.nodeSuccess = shardResults.nodesSuccess
      result.nodeSuccessInfo = shardResults.nodesSuccessInfo
      result.nodeError = shardResults.nodesError
    } else {
      results = Array(searchResults)
      result = SearchResult.mergeFacet(field, results.filterNot(_ == null))
      result.nodeAll = 1
      result.nodeSuccess = 1

    }

    result
  }

  def search(q: String, start: Int, offset: Int, sortStr: String): SearchResult = {
    //进行搜索的时候，为了提高缓存的命中率，提高offset
    var topN = start + offset
    if (topN < 1000) topN = 1000
    val searchResults = resourceSearcher.collectSearch(rd.name, q, sortStr, topN)
    internalSearch(searchResults, start, offset)
  }

  private def internalSearch(searchResults: ShardResult, start: Int, offset: Int): SearchResult = {
    var results: Array[ShardResult] = null
    var result: SearchResult = null
    if (searchResults.isInstanceOf[ShardResultCollect]) {
      //集群查询情况下读取所有的值
      val shardResults = searchResults.asInstanceOf[ShardResultCollect]
      results = shardResults.shardResults
      result = SearchResult.merge(start, offset, results.filterNot(_ == null))
      result.nodeAll = shardResults.nodesAll
      result.nodeSuccess = shardResults.nodesSuccess
      result.nodeSuccessInfo = shardResults.nodesSuccessInfo
      result.nodeError = shardResults.nodesError
    } else {
      results = Array(searchResults)
      result = SearchResult.merge(start, offset, results.filterNot(_ == null))
      result.nodeAll = 1
      result.nodeSuccess = 1

    }

    result
  }

  def search2(q: String, start: Int, offset: Int, sortStr: String): SearchResult = {
    val searchResults = resourceSearcher.collectSearch2(rd.name, q, sortStr, start + offset)
    internalSearch(searchResults, start, offset)
  }

  def idSearch(q: String) = {
    resourceSearcher.searchObjectId(rd.name, q)
  }

  def createHighlighter(q: String): (Highlighter, Analyzer) = {
    val parser = new MultiFieldQueryParser(this.defaultSearchFields, analyzer)
    val query = parser.parse(q)
    (new Highlighter(new SimpleHTMLFormatter(MonadFaceConstants.HIGHLIGHT_PREFIX,
      MonadFaceConstants.HIGHLIGHT_SUFFIX),
      new QueryTermScorer(query)), this.analyzer)
  }

  def getDocumentNum: Int = {
    resourceSearcher.maxDoc(rd.name)
  }

  def start() {
    logger.info("[{}] start search queue", rd.name)
  }

  def shutdown() {
    logger.info("[{}] shutdown search queue", rd.name)
  }
}

