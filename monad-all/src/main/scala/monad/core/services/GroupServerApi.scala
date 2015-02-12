// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.services

import monad.core.model.GroupConfig
import monad.face.model.ResourceDefinition

/**
 * 针对本地组服务器操作API
 * @author jcai
 */
trait GroupServerApi {
  /**
   * 得到自己组的配置
   * @return 自身组的配置
   */
  def GetSelfGroupConfig: GroupConfig

  /**
   * 得到其他组的配置
   * @return 其他组的配置
   */
  def GetOtherGroups: List[GroupConfig]

  /**
   * 得到所有资源的集合
   * @param group 组名称
   * @return 所有资源列表
   */
  def GetResources(group: Option[String] = None): List[ResourceDefinition]

  /**
   * 得到云服务器地址
   * @return 云服务器地址
   */
  def GetCloudAddress: String
}
