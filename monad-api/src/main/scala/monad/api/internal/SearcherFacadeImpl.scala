// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import java.io.StringReader
import java.util.concurrent.{Semaphore, TimeUnit}

import com.google.gson.JsonObject
import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, SearcherFacade, SearcherQueue}
import monad.face.config.ApiConfigSupport
import stark.utils.services.StarkException
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * 搜索的实现
 * @author jcai
 */
class SearcherFacadeImpl(extractor: SearchResultExtractor, searcherQueue: SearcherQueue, apiConfig: ApiConfigSupport) extends SearcherFacade {
  private final val ONE_MINUTE = 60
  private val semaphore = new Semaphore(apiConfig.api.concurrentQuery)

  def getDocumentNum: Long = searcherQueue.getDocumentNum

  def facetSearch(searchRequest: SearchRequest): JsonObject = {
    extractor.extract(searchRequest, { request =>
      searcherQueue.facetSearch(request.q, searchRequest.facetField, searchRequest.facetUpper, searchRequest.facetLower)
    })
  }

  def search(searchRequest: SearchRequest): JsonObject = {
    doInSearcherQueue {
      var highlighter: Option[ResultHighlighter] = None
      //当需要高亮并且有q的时候，才进行高亮显示
      if (searchRequest.hl && !InternalUtils.isBlank(searchRequest.q)) {
        highlighter = Some(new ResultHighlighter {
          private lazy val (highlighter, analyzer) = {
            searcherQueue.createHighlighter(searchRequest.q)
          }

          def highlight(fieldName: String, text: String, maxNumFragments: Int) = {
            val tokenStream = analyzer.tokenStream(fieldName, new StringReader(text))
            val result = highlighter.getBestFragments(tokenStream, text, maxNumFragments, "......")
            if (InternalUtils.isBlank(result)) text else result
          }
        })
      }
      return extractor.extract(searchRequest, { request =>
        searcherQueue.search(request.q, request.start, request.offset, request.sort)
      }, highlighter)
    }
    throw new StarkException("经过60s后未能获取搜索对象", MonadApiExceptionCode.HIGH_CONCURRENT)
  }

  private def doInSearcherQueue[T](fun: => T): T = {
    if (semaphore.tryAcquire(ONE_MINUTE, TimeUnit.SECONDS)) {
      try {
        fun
      } finally {
        semaphore.release()
      }
    }else{
      throw new StarkException("经过60s后未能获取搜索对象", MonadApiExceptionCode.HIGH_CONCURRENT)
    }
  }

  /*
  def getBestFragments(indexName:String,highlighter:Highlighter,fieldName:String,text:String,maxNumFragments:Int):String= {
      if(text eq null)
          return null
      highlighter.getFragmentScorer
      val searcherQueue = findSearcherQueue(indexName);
      val analyzer = searcherQueue.defaultAnalyzer
      val tokenStream = analyzer.tokenStream(fieldName, new StringReader(text));
      val result = highlighter.getBestFragments(tokenStream,text, maxNumFragments,"......")
      if(!StringUtils.hasText(result)) {text} else {result}
  }
  */
  /**
   * 针对id的搜索服务
   * @param searchRequest 搜索请求
   * @return
   */
  def idSearch(searchRequest: SearchRequest) = {
    doInSearcherQueue{
      searcherQueue.idSearch(searchRequest.q)
    }
  }
}

