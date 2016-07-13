// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.model

import monad.api.services.DBObjectExtractor
import roar.api.meta.ResourceDefinition

/**
 * 搜索请求
 *
 * @author jcai
 */
class SearchRequest {
  /** 资源名称 **/
  var resourceName: String = _
  var resource: ResourceDefinition = _
  /** 搜索的关键字 **/
  var q: String = _
  /** 搜索的ObjectId,通常为查询某一具体的数据 **/
  var objectId: Array[Byte] = _
  /** 搜索排序 **/
  var sort: String = _
  /** 搜索的起始位置 **/
  var start: Int = 0
  /** 搜索的偏移量 **/
  var offset: Int = 0
  /** 是否需要highlight **/
  var hl:Boolean = true
  /** 是否要得到结果数据 **/
  var includeData: Boolean = true
  /** 针对结果的处理 **/
  var dbObjectExtractor: Option[DBObjectExtractor] = None

  /** 针对facet搜索时候，提供的统计字段 **/
  var facetField: String = _
  //频次的上限
  var facetUpper: Int = -1
  //频次的下限
  var facetLower: Int = -1
}
