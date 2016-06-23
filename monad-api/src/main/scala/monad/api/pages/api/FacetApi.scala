// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.internal.SearchResultExtractor
import monad.api.model.SearchRequest
import monad.api.services.MonadApiExceptionCode
import stark.utils.services.StarkException
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request

import scala.collection.JavaConversions._

/**
 * facet api
 * @author jcai
 */

class FacetApi extends TraceApi {
  @Inject
  private var request: Request = _

  override protected def search(searchRequest: SearchRequest) = {
    val field = request.getParameter("f")
    if (InternalUtils.isBlank(field)) {
      throw new StarkException("身份证号(f)为空", MonadApiExceptionCode.ID_CARD_IS_NULL)
    }
    //转换为本资源的属性
    val uniqueFieldOption = searchRequest.resource.
      dynamicType.properties.find(_.traitProperty == field)
    if (uniqueFieldOption.isEmpty) {
      throw new StarkException("[" + searchRequest.resourceName + "]通过" + field + "未能找到找到资源属性",
        MonadApiExceptionCode.RESOURCE_NOT_FOUND
      )
    }
    searchRequest.facetField = uniqueFieldOption.get.name

    //频次上限
    val facetUpper = request.getParameter("u")
    if (!InternalUtils.isBlank(facetUpper)) {
      searchRequest.facetUpper = facetUpper.toInt
    }

    //频次下限
    val facetLower = request.getParameter("l")
    if (!InternalUtils.isBlank(facetLower)) {
      searchRequest.facetLower = facetLower.toInt
    }

    //转换为动态数据资源格式
    searchRequest.dbObjectExtractor = Some(SearchResultExtractor.DynamicDBObjectExtractor)
    getSearchFacade.facetSearch(searchRequest)
  }
}
