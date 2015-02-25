package monad.api.pages.api.analytics

import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, SearcherFacade}
import monad.face.services.ResourceDefinitionLoader
import monad.support.services.MonadException
import org.apache.tapestry5.ioc.Messages
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request
import org.slf4j.LoggerFactory

/**
 * 搜索人的ID
 * @author jcai
 */
class IdSearcherApi {
  private val logger = LoggerFactory getLogger getClass
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
    /*
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
    logger.info("[" + searchRequest.resourceName + "] idsearch q:[{}],time:{}ms", q, (end - begin))

    return r.asInstanceOf[IdShardResultCollect]
    */
  }

  protected def initResource(searchRequest: SearchRequest) {
    //index name
    val indexName = request.getParameter("i")
    if (InternalUtils.isBlank(indexName)) {
      throw new MonadException(messages.get("invalidate-parameter"),
        MonadApiExceptionCode.MISSING_RESOURCE_PARAMETER
      )
    }
    searchRequest.resourceName = indexName
    //get column definitions
    val definition = loader.getResourceDefinition(indexName)
    if (definition.isEmpty) {
      throw new MonadException(messages.get("fail-to-find-index"),
        MonadApiExceptionCode.RESOURCE_NOT_FOUND
      )
    }
    searchRequest.resource = definition.get
  }
}
