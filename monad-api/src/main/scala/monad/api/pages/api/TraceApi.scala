// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.internal.SearchResultExtractor
import monad.api.model.SearchRequest
import monad.api.services.{DynamicTraceService, MonadApiExceptionCode}
import monad.face.services.ResourceDefinitionLoader
import stark.utils.services.StarkException
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request

import scala.collection.JavaConversions._

/**
 * 动态轨迹查询的API
 * @author jcai
 */

class TraceApi extends SearchApi {
  @Inject
  private var request: Request = _
  @Inject
  private var dynamicTraceService: DynamicTraceService = _

  @Inject
  private var loader: ResourceDefinitionLoader = _

  override protected def createQuery = {
    //get column definitions
    val definition = loader.getResourceDefinition(request.getParameter("i"))
    if (definition.isEmpty) {
      throw new StarkException("不能发现Resource!",
        MonadApiExceptionCode.RESOURCE_NOT_FOUND
      )
    }
    //是否为动态资源
    val resource = definition.get
    if (!resource.dynamic) {
      throw new StarkException("资源" + resource.name + "不是动态资源，请检查定义!",
        MonadApiExceptionCode.INVALIDATE_DYNAMIC_RESOURCE
      )
    }

    val dynamicDefinition = resource.dynamicType.properties

    val stringBuilder = new StringBuilder
    val keyword = request.getParameter("q")
    if (!InternalUtils.isBlank(keyword)) {
      stringBuilder.append(keyword).append(" ")
    }
    dynamicDefinition.foreach { pro =>
      val v = request.getParameter(pro.traitProperty)
      if (!InternalUtils.isBlank(v)) {
        stringBuilder.append(pro.name + ":" + v + " ")
      }
    }

    val q = stringBuilder.toString().trim
    if (InternalUtils.isBlank(q)) {
      throw new StarkException("搜索资源[%s]的参数为空，请检查特征列参数的传递".format(resource.name),
        MonadApiExceptionCode.MISSING_QUERY_PARAMETER
      )
    }

    q
  }

  override protected def search(searchRequest: SearchRequest) = {
    searchRequest.dbObjectExtractor = Some(SearchResultExtractor.DynamicDBObjectExtractor)
    super.search(searchRequest)
  }
}
