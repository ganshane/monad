// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.services


import com.google.gson.JsonObject
import monad.api.model.SearchRequest

/**
 * 所有搜索的管理
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.1
 */
trait SearcherFacade {
  /**
   * 得到文档数目
   * @return 文档数目
   * @since 0.1
   */
  def getDocumentNum: Long

  /**
   * 搜索
   * @return 结果列表
   * @since 0.1
   */
  def search(searchRequest: SearchRequest): JsonObject

  def search2(searchRequest: SearchRequest): JsonObject

  /**
   * 针对id的搜索服务
   * @param searchRequest 搜索请求
   * @return
   */
  //def idSearch(searchRequest: SearchRequest): IdShardResult

  /**
   * 支持针对某一字段的频次分析
   * @param searchRequest 搜索的请求对象
   * @return 搜索结果
   */
  def facetSearch(searchRequest: SearchRequest): JsonObject
}
