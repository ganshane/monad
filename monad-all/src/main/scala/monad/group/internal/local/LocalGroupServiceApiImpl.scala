// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.group.internal.local

import monad.face.config.{GroupConfigSupport, CloudServerSupport}
import monad.face.model.{GroupConfig, ResourceDefinition}
import monad.face.services.GroupServerApi
import monad.group.internal.MonadGroupUpNotifier
import monad.support.services.XmlLoader

/**
 * 本地化的monad组配置
 * @author jcai
 */
class LocalGroupServiceApiImpl(config: GroupConfigSupport, cloudManager: MonadGroupUpNotifier) extends GroupServerApi {
  /**
   * 得到自己组的配置
   * @return 自身组的配置
   */
  def GetSelfGroupConfig = {
    val groupConfig = new GroupConfig
    groupConfig.id = config.group.id
    groupConfig.cnName = config.group.cnName
    groupConfig.apiUrl = config.group.apiUrl

    groupConfig
  }

  /**
   * 得到其他组的配置
   * @return 其他组的配置
   */
  def GetOtherGroups = cloudManager.getLiveGroups

  /**
   * 得到所有资源的集合
   * @param group 组名称
   * @return 所有资源列表
   */
  def GetResources(group: Option[String]) = {
    cloudManager.findResourcesContent(group.getOrElse("")).map { xmlContent =>
      XmlLoader.parseXML[ResourceDefinition](xmlContent)
    }
  }


  /**
   * 得到云服务器地址
   * @return 云服务器地址
   */
  def GetCloudAddress = config.asInstanceOf[CloudServerSupport].cloudServer

}
