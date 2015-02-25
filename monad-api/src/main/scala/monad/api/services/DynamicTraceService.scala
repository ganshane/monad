// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.services

import monad.face.model.DynamicResourceDefinition

/**
 * 动态轨迹的服务类
 * @author jcai
 */

trait DynamicTraceService {
  /**
   * 得到所有动态轨迹资源列表
   * @return 动态轨迹列表
   */
  def getDynamicResource: Iterator[String]

  /**
   * 得到动态资源定义
   * @return 动态资源定义
   */
  def getDynamicResourceDefinition: DynamicResourceDefinition
}
