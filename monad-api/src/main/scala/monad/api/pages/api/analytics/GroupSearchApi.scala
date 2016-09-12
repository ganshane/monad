package monad.api.pages.api.analytics

import java.util.Date

import monad.api.MonadApiConstants
import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, SearcherFacade}
import monad.face.services.ResourceDefinitionLoader
import org.apache.tapestry5.ioc.Messages
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request
import org.apache.tapestry5.util.TextStreamResponse
import stark.utils.services.{LoggerSupport, StarkException}

/**
  * group search api
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2016-09-12
  */
class GroupSearchApi extends LoggerSupport{
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
    val g = request.getParameter("g")
    searchRequest.facetField = g
    logger.info("[" + searchRequest.resourceName + "] group search q:[{}] g:[{}]....", q,g)
    val begin = new Date().getTime
    val r = searchFacade.facetSearch(searchRequest)
    val end = new Date().getTime
    info("[" + searchRequest.resourceName + "] group search q:[{}], time:{}ms ", q,end - begin)

    new TextStreamResponse(MonadApiConstants.JSON_MIME_TYPE, r.toString)
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
