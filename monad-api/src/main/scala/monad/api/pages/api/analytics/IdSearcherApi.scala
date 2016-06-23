// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api.analytics

import java.util.Date

import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, SearcherFacade}
import monad.face.services.ResourceDefinitionLoader
import stark.utils.services.{LoggerSupport, StarkException}
import org.apache.tapestry5.ioc.Messages
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request

/**
 * 搜索人的ID
 * @author jcai
 */
class IdSearcherApi extends LoggerSupport{
  //searcher facade object
  @Inject
  private var searchFacade: SearcherFacade = _
  @Inject
  private var loader: ResourceDefinitionLoader = _
  @Inject
  private var request: Request = _
  @Inject
  private var messages: Messages = _

  def onActivate() = {
    val searchRequest = new SearchRequest
    searchRequest.includeData = false
    //search query keyword
    val q = request.getParameter("q")
    searchRequest.q = q
    initResource(searchRequest)
    logger.info("[" + searchRequest.resourceName + "] idsearch q:[{}] ....", q)
    val begin = new Date().getTime
    val r = searchFacade.idSearch(searchRequest)
    val end = new Date().getTime
    info("[" + searchRequest.resourceName + "] idsearch q:[{}],hits:{} time:{}ms ", q,r.data.cardinality(),(end - begin))

    r
  }

  protected def initResource(searchRequest: SearchRequest) {
    //index name
    val indexName = request.getParameter("i")
    if (InternalUtils.isBlank(indexName)) {
      throw new StarkException(messages.get("invalidate-parameter"),
        MonadApiExceptionCode.MISSING_RESOURCE_PARAMETER
      )
    }
    searchRequest.resourceName = indexName
    //get column definitions
    val definition = loader.getResourceDefinition(indexName)
    if (definition.isEmpty) {
      throw new StarkException(messages.get("fail-to-find-index"),
        MonadApiExceptionCode.RESOURCE_NOT_FOUND
      )
    }
    searchRequest.resource = definition.get
  }
}
