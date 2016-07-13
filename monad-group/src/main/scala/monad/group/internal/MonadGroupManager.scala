// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.internal

import java.text.SimpleDateFormat
import java.util.Date

import monad.core.config.ZkClientConfigSupport
import monad.face.CloudPathConstants
import monad.face.model.{DynamicResourceDefinition, ResourceRelation}
import monad.face.services.{DataTypeUtils, GroupZookeeperTemplate}
import org.slf4j.LoggerFactory
import roar.api.meta.ResourceDefinition
import stark.utils.StarkUtilsConstants
import stark.utils.services.XmlLoader

import scala.util.control.NonFatal

/**
 * monad group manager
 * group的文件结构如下：
 * /groups/xx 为某一组的目录
 * /groups/xx/resources/yy 为组内某一资源的数据定义
 * /groups/xx/resources/yy/max 为组内某一资源的数据的最大值
 * /groups/xx/relations 为组内关系定义
 *
 * @author jcai
 */
class MonadGroupManager(config: ZkClientConfigSupport, zk: GroupZookeeperTemplate) {
  private val logger = LoggerFactory getLogger getClass

  /**
   * 保存或更新资源
   */
  def saveOrUpdateResource(resource: ResourceDefinition, xml: Option[String] = None) {
    saveOrUpdate("/resources/" + resource.name, resource, xml)
  }

  def resync(resourceName: String) {
    zk.resync(resourceName)
  }

  /**
   * 查询所有的资源
   */
  def findResources = {
    zk.getChildren("/resources").map(x => zk.getDataAsString("/resources" + '/' + x)).
      filter(_.isDefined).
      map(y => {
      try {
        XmlLoader.parseXML[ResourceDefinition](y.get)
      } catch {
        case NonFatal(e) =>
          logger.warn("不能解析XML", e)
          null
      }
    }).filterNot(_ == null)
  }

  /**
   * delete resource
   */
  def deleteResources(name: String) {
    zk deleteRecursive CloudPathConstants.RESOURCE_PATH_FORMAT.format(name)
  }

  def getResource(name: String) = getXmlDefinition(CloudPathConstants.RESOURCE_PATH_FORMAT.format(name))

  def getXmlDefinition(path: String) = zk.getDataAsString(path).getOrElse("")

  def getDynamic = getXmlDefinition(CloudPathConstants.DYNAMIC_PATH)

  def getRelation = getXmlDefinition(CloudPathConstants.RELATION_PATH)

  def getStat(name: String): (Int, String) = {
    var v = zk.getData(CloudPathConstants.RESOURCE_ID_PATH_FORMAT.format(name))
    val total = v match {
      case Some(x) =>
        if (x.length == 4)
          DataTypeUtils.convertAsInt(x)
        else
          0
      case o => 0
    }
    v = zk.getData(CloudPathConstants.RESOURCE_MAX_PATH_FORMAT.format(name))
    val formatter = new SimpleDateFormat("yyyyMMddHHmmss")
    val maxValue = v match {
      case Some(xx) =>
        val l = DataTypeUtils.convertAsLong(v).get
        formatter.format(new Date(l)) + "(" + l + ")"
      case o =>
        "unknown"
    }
    (total, maxValue)
  }

  /**
   * 保存关联关系
   */
  def saveOrUpdateRelation(resource: ResourceRelation, xml: Option[String]) {
    saveOrUpdate(CloudPathConstants.RELATION_PATH, resource, xml)
  }

  def saveOrUpdateDynamic(resource: DynamicResourceDefinition, xml: Option[String]) {
    saveOrUpdate(CloudPathConstants.DYNAMIC_PATH, resource, xml)
  }

  private def saveOrUpdate[T](path: String, obj: T, xml: Option[String]) {
    val stat = zk.stat(path)
    val str = xml.getOrElse(XmlLoader.toXml(obj))
    if (stat.isDefined) {
      zk.setStringData(path, str)
    } else {
      zk.createPersistPath(path, Some(str.getBytes(StarkUtilsConstants.UTF8_ENCODING))
      )
    }

  }
}
