// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import javax.annotation.PostConstruct

import com.google.gson.JsonObject
import monad.face.{CloudPathConstants, MonadFaceConstants}
import monad.support.services.ZookeeperTemplate
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.Stat

/**
 * 基于组的zookeeper模板类
 * @author jcai
 */
class GroupZookeeperTemplate(groupApi: GroupServerApi, periodExecutor: PeriodicExecutor)
  extends ZookeeperTemplate(groupApi.GetCloudAddress, Some(CloudPathConstants.GROUPS_PATH + "/" + groupApi.GetSelfGroupConfig.id)) {

  /**
   * 启动对象实例
   */
  @PostConstruct
  override def start(hub: RegistryShutdownHub): Unit = {
    super.start(hub)
    setupGroupDirectoryInZk()
    startCheckFailed(periodExecutor)
  }

  def setupGroupDirectoryInZk() = {
    createPersistPath(CloudPathConstants.RESOURCES_PATH)
    createPersistPath(CloudPathConstants.NODE_PATH_FORMAT)
    createPersistPath(CloudPathConstants.DYNAMIC_PATH)
    createPersistPath(CloudPathConstants.RELATION_PATH)
  }

  def getNodeInstance: Seq[String] = {
    getChildren(CloudPathConstants.NODE_PATH_FORMAT).foldLeft(Seq[String]()) { (seq, path) =>
      val value = getDataAsString(CloudPathConstants.NODE_PATH_FORMAT + "/" + path)
      if (value.isDefined) seq :+ value.get else seq
    }
  }

  def resync(resourceName: String) {
    val resyncPath = CloudPathConstants.RESOURCE_RESYNC_PATH_FORMAT.format(resourceName)
    try {
      createPersistPath(resyncPath)
      val resourcePath = CloudPathConstants.RESOURCE_PATH_FORMAT.format(resourceName)
      val oldContent = getData(resourcePath)
      if (oldContent.isDefined)
        setData(resourcePath, oldContent.get)
      Thread.sleep(5000)
    } finally {
      delete(resyncPath)
    }
  }

  def getRegionInfo(resourceName: String): JsonObject = {
    val path = CloudPathConstants.RESOURCE_REGION_INFO_PATH_FORMAT.format(resourceName)
    var json: JsonObject = null
    val data = getDataAsString(path)
    data match {
      case Some(str) =>
        json = parseJson(str)
      case _ =>
        json = new JsonObject
    }

    json
  }

  private def parseJson(data: String): JsonObject = {
    try {
      return MonadFaceConstants.GLOBAL_GSON_PARSER.parse(data).getAsJsonObject
    } catch {
      case other: Throwable =>
        return new JsonObject
    }
  }

  def setRegionSyncInfo(resourceName: String, regionSyncInfo: JsonObject) {
    val path = CloudPathConstants.RESOURCE_REGION_INFO_PATH_FORMAT.format(resourceName)
    val stat = new Stat
    stat.setVersion(-1)
    val data = getDataAsString(path, Some(stat))
    if (stat.getVersion > -1) {

      var json = new JsonObject
      if (data.isDefined)
        json = parseJson(data.get)
      json.add("sync", regionSyncInfo)
      val newData = MonadFaceConstants.GLOBAL_GSON.toJson(json)
      try {
        setStringData(path, newData, Some(stat))
      } catch {
        case e: KeeperException.BadVersionException =>
          setRegionSyncInfo(resourceName, regionSyncInfo)
      }
    } else {
      //没有节点
      createPersistPath(path)
      setRegionSyncInfo(resourceName, regionSyncInfo)
    }
  }

  def setRegionIndexInfo(resourceName: String, regionSeq: Int, regionIndexInfo: JsonObject) {
    val path = CloudPathConstants.RESOURCE_REGION_INFO_PATH_FORMAT.format(resourceName)
    val stat = new Stat
    stat.setVersion(-1)
    val data = getDataAsString(path, Some(stat))
    if (stat.getVersion > -1) {
      var json = new JsonObject
      if (data.isDefined)
        json = parseJson(data.get)
      json.add("index-region-" + regionSeq, regionIndexInfo)
      val newData = MonadFaceConstants.GLOBAL_GSON.toJson(json)
      try {
        setStringData(path, newData, Some(stat))
      } catch {
        case e: KeeperException.BadVersionException =>
          setRegionIndexInfo(resourceName, regionSeq, regionIndexInfo)
      }
    } else {
      //没有节点
      createPersistPath(path)
      setRegionIndexInfo(resourceName, regionSeq, regionIndexInfo)
    }
  }
}
