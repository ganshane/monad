// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.group.internal

import com.google.gson.Gson
import monad.face.CloudPathConstants
import monad.face.config.{GroupConfigSupport, CloudServerSupport}
import monad.face.model.GroupConfig
import monad.support.MonadSupportConstants
import monad.support.services.{ServiceLifecycle, ServiceUtils, ZookeeperTemplate}
import org.apache.tapestry5.ioc.Invokable
import org.apache.tapestry5.ioc.services.ParallelExecutor
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor
import org.slf4j.LoggerFactory

/**
 * Cluster的管理类，主要针对Cluster的服务操作
 * @author jcai
 */
class MonadGroupUpNotifier(config: GroupConfigSupport, periodExecutor: PeriodicExecutor, parallelExecutor: ParallelExecutor)
  extends ServiceLifecycle {
  private val logger = LoggerFactory getLogger getClass
  //create base directory
  private var rootZk: ZookeeperTemplate = null

  /**
   * 启动对象实例
   */
  def start() {
    if (parallelExecutor == null) {
      internalStart()
    } else {
      parallelExecutor.invoke(new Invokable[Unit] {
        def invoke() {
          internalStart()
        }
      })
    }
  }

  private def internalStart() {
    rootZk = new ZookeeperTemplate(config.asInstanceOf[CloudServerSupport].cloudServer)
    rootZk.start()
    rootZk.startCheckFailed(periodExecutor)
    ServiceUtils.runInNoThrow {
      initLive(rootZk)
    }
    notifyGroupUp()
  }

  def initLive(zookeeper: ZookeeperTemplate) {
    logger.info("notify group [{}] server up", config.group.cnName)
    val groupConfig = new GroupConfig
    groupConfig.id = config.group.id
    groupConfig.cnName = config.group.cnName
    groupConfig.apiUrl = config.group.apiUrl
    val jsonObject = new Gson().toJson(groupConfig)
    val stat = zookeeper.stat(CloudPathConstants.LIVE_PATH)
    if (stat.isEmpty)
      zookeeper.createPersistPath(CloudPathConstants.LIVE_PATH)
    //watch the node,当失去连接，或者session过期的时候，能够自动创建
    zookeeper.createEphemeralPath(CloudPathConstants.LIVE_PATH + "/" + config.group.id,
      Some(jsonObject.getBytes(MonadSupportConstants.UTF8_ENCODING)))
  }

  /**
   * 通知一下我自己已经起来了
   */
  private def notifyGroupUp() {
    val groupPath = CloudPathConstants.GROUPS_PATH + "/" + config.group.id
    rootZk.createPersistPath(groupPath, Some(config.group.cnName.getBytes(MonadSupportConstants.UTF8_ENCODING)))
    val groupResourcesPath = CloudPathConstants.GROUPS_PATH + "/" + config.group.id + CloudPathConstants.RESOURCES_PATH
    rootZk.createPersistPath(groupResourcesPath)
    val groupNodesPath = CloudPathConstants.GROUPS_PATH + "/" + config.group.id + CloudPathConstants.NODE_PATH_FORMAT
    rootZk.createPersistPath(groupNodesPath)
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    if (rootZk != null) rootZk.shutdown()
  }

  def getLiveGroups = {
    rootZk.getChildren(CloudPathConstants.LIVE_PATH).foldLeft(List[GroupConfig]()) { (list, path) =>
      val value = rootZk.getDataAsString(CloudPathConstants.LIVE_PATH + "/" + path)
      if (value.isDefined) {
        list :+ new Gson().fromJson(value.get, classOf[GroupConfig])
      } else {
        list
      }
    }
  }

  /**
   * 查询某一组的资源
   * @param group 组的名称
   */
  def findResourcesContent(group: String): List[String] = {
    val groupResourcesPath = CloudPathConstants.GROUPS_PATH + "/" + group + "/resources"
    rootZk.
      getChildren(groupResourcesPath).
      map(x => rootZk.getDataAsString(groupResourcesPath + "/" + x)).
      filter(x => x.isDefined && !x.get.isEmpty).map(_.get).toList
  }

  //only for test
  private[internal] def expireSession() {
    rootZk.buildAnotherSession.close()
  }
}
