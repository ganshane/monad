// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.model.SearchRequest
import monad.api.services.{MonadApiExceptionCode, RelationService, ResourceRequest}
import monad.face.model.ResourceRelation
import monad.face.services.ResourceDefinitionLoader
import stark.utils.services.StarkException
import org.apache.tapestry5.annotations.Property
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request

import scala.collection.JavaConversions._

/**
 * 查询关系的API
 * 请求的参数有：
 * r 关系名称
 * start 起始查询位置
 * offset 查询偏移量
 * @author jcai
 */
class RelationApi extends SearchApi {
  @Inject
  private var request: Request = _
  @Inject
  private var relationService: RelationService = _
  @Inject
  private var loader: ResourceDefinitionLoader = _
  @Property
  private var rel: ResourceRelation.Rel = _
  @Inject
  private var resourceRequest: ResourceRequest = _

  override protected def createQuery = {
    val relationId = request.getParameter("r")
    if (InternalUtils.isBlank(relationId)) {
      throw new StarkException("参数r(关系)是空值!", MonadApiExceptionCode.MISSING_RELATION_PARAMETER)
    }

    val relation = relationService.findRelation(relationId)
    if (relation.isEmpty) {
      throw new StarkException("通过%s未能找到关系定义".format(relationId), MonadApiExceptionCode.RELATION_NOT_FOUND)
    }

    rel = relation.get


    //构造查询参数
    rel.properties.map { pro =>
      val v = request.getParameter(pro.traitProperty)
      if (InternalUtils.isBlank(v)) {
        throw new StarkException(
          "关系属性中的参数%s为空".format(pro.traitProperty),
          MonadApiExceptionCode.MISSING_RELATION_PROPERTY_PARAMETER
        )
      }
      pro.name + ":" + v
    }.mkString(" ").trim
  }

  override protected def initResource(searchRequest: SearchRequest) {
    searchRequest.resourceName = rel.resource
    //get column definitions
    var definition = loader.getResourceDefinition(rel.resource)
    if (definition.isEmpty) {
      throw new StarkException(
        "通过%s未能找到资源定义,检查关系定义".format(rel.resource),
        MonadApiExceptionCode.RELATION_NOT_FOUND
      )
    }
    resourceRequest.storeResourceDefinition(definition.get)
    searchRequest.resource = definition.get
  }
}
