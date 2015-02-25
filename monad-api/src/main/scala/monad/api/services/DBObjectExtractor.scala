// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.services

import com.google.gson.JsonObject
import monad.api.internal.ResultHighlighter
import monad.face.model.ResourceDefinition

/**
 * 针对查询的数据进行解析成前端API需要的数据格式
 * @author jcai
 */
trait DBObjectExtractor {
  /**
   * 解析数据
   * @param resource 资源定义
   * @param row 数据
   * @param highlighterObj 高亮显示的对象
   * @return 解析后的结果
   */
  def extract(resource: ResourceDefinition, row: JsonObject, highlighterObj: Option[ResultHighlighter] = None): Option[JsonObject]
}
