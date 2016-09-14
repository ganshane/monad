package monad.api.pages.api.analytics

import java.util.Date

import com.google.gson.JsonObject
import monad.api.base.BaseApi
import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, SearcherFacade}
import monad.face.services.ResourceDefinitionLoader
import org.apache.tapestry5.ioc.Messages
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request
import stark.utils.services.{LoggerSupport, StarkException}

/**
  * group search api
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2016-09-12
  */
class GroupSearchApi extends BaseApi with LoggerSupport{
  @Inject
  private var searchFacade: SearcherFacade = _
  @Inject
  private var loader: ResourceDefinitionLoader = _
  @Inject
  private var request: Request = _
  @Inject
  private var messages: Messages = _


  /**
    * 执行对应的ＡＰＩ
    *
    * @return api return result
    * @since 0.1
    */
  override protected def doExecuteApi(): JsonObject = {
    val searchRequest = new SearchRequest
    searchRequest.includeData = false
    //search query keyword
    val q = request.getParameter("q")
    searchRequest.q = q
    initResource(searchRequest)
    val g = request.getParameter("g")
    searchRequest.facetField = g
    val minFreqString = request.getParameter("m")
    val minFreq = if(minFreqString == null) 1 else minFreqString.toInt
    val topNString = request.getParameter("top")
    val topN = if(topNString == null) 30 else  topNString.toInt
    logger.info("[" + searchRequest.resourceName + "] group search q:[{}] g:[{}]....", q,g)
    val begin = new Date().getTime
    val r = searchFacade.facetSearch(searchRequest,minFreq,topN)
    val end = new Date().getTime
    info("[" + searchRequest.resourceName + "] group search q:[{}], time:{}ms ", q,end - begin)
    r.addProperty("time",end-begin)

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
