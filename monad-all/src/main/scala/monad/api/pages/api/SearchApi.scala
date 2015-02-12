// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai. 
 * site: http://www.ganshane.com
 */

package monad.api.pages.api

import com.google.gson.JsonObject
import monad.api.base.BaseApi
import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, SearcherFacade}
import monad.core.services.ObjectIdCreator
import monad.face.services.ResourceDefinitionLoader
import monad.support.services.MonadException
import org.apache.tapestry5.ioc.Messages
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.{Request, ValueEncoderSource}
import org.slf4j.Logger

/**
 * main query api.
 * <p>
 * all query api can use it as base object
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.1
 */
class SearchApi extends BaseApi {
  //searcher facade object
  @Inject
  private var searchFacade: SearcherFacade = _
  //web request
  @Inject
  private var request: Request = _
  @Inject
  private var valueEncoderSource: ValueEncoderSource = _
  @Inject
  private var messages: Messages = _
  @Inject
  private var logger: Logger = _

  @Inject
  private var loader: ResourceDefinitionLoader = _
  @Inject
  private var objectIdCreator: ObjectIdCreator = _

  protected def doExecuteApi(): JsonObject = query(includeData = true)

  protected def query(includeData: Boolean): JsonObject = {
    val searchRequest = new SearchRequest
    searchRequest.includeData = includeData
    //search query keyword
    var q = createQuery
    //some doucment primary key  if search one document
    val id = request.getParameter("id")
    val objectId = request.getParameter("_id")
    if (objectId != null)
      searchRequest.objectId = objectIdCreator.stringToObjectId(objectId)
    //start position
    val startStr = request.getParameter("start")
    //offset number
    val offsetStr = request.getParameter("offset")
    val limitStr = request.getParameter("limit")
    //sort
    val sortStr = request.getParameter("sort")
    searchRequest.sort = sortStr

    initResource(searchRequest)
    /*
    //兼容老的API，查询ID
    if (! InternalUtils.isBlank(id)){
        //primary key column
        val primaryKeyColumn = searchRequest.resource.properties.filter(_.primaryKey) head;
        q = "%s:%s".format(primaryKeyColumn.name, id)
    }
    */
    searchRequest.q = q
    if (InternalUtils.isBlank(q) && InternalUtils.isBlank(objectId)) {
      throw new MonadException(messages.get("invalidate-query-parameter"),
        MonadApiExceptionCode.MISSING_QUERY_PARAMETER
      )
    }

    //start position
    var start: Int = 0
    //convert as integer
    val intValueEncoder = valueEncoderSource.getValueEncoder(classOf[Int])
    if (!InternalUtils.isBlank(startStr)) {
      start = intValueEncoder.toValue(startStr)
    }
    searchRequest.start = start
    //offset,default 10
    var offset = 10
    if (!InternalUtils.isBlank(limitStr)) {
      offset = intValueEncoder.toValue(limitStr)
    }
    if (!InternalUtils.isBlank(offsetStr)) {
      offset = intValueEncoder.toValue(offsetStr)
    }
    if (offset > 10000) {
      offset = 10000
    }
    searchRequest.offset = offset

    search(searchRequest)
  }

  protected def createQuery: String = request.getParameter("q")

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

  protected def search(searchRequest: SearchRequest) = searchFacade.search(searchRequest)

  protected def getSearchFacade = searchFacade
}
