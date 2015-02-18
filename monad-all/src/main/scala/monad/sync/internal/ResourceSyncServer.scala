package monad.sync.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import monad.face.config.SyncConfigSupport
import monad.face.model.ResourceDefinition
import monad.face.services.GroupZookeeperTemplate
import monad.jni.{SyncIdNoSQL, SyncServer}
import monad.support.services.ServiceLifecycle

/**
 * 资源同步服务管理器
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class ResourceSyncServer(syncConfig: SyncConfigSupport, zk: GroupZookeeperTemplate)
  extends SyncServer
  with ServiceLifecycle {
  private val savers = new ConcurrentHashMap[String, ResourceSaver]()
  private val createLock = new ReentrantLock()

  /**
   * 启动对象实例
   */
  def start() {
    Start(syncConfig.sync.bind)
    StartWorkers(syncConfig.sync.sync_thread_num)
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    //停止所有的NoSQL服务器
    val it = savers.values().iterator()
    while (it.hasNext) {
      it.next().stop()
    }
    //停止本身的服务器
    StopServer()
    delete()
  }

  private[internal] def createResourceSaverIfPresent(rd: ResourceDefinition,
                                                     version: Int,
                                                     delegateFinder: Option[String => Option[ResourceSaver]] = None,
                                                     idNoSQL: Option[SyncIdNoSQL] = None): ResourceSaver = {
    var saver = savers.get(rd.name)

    if (saver != null)
      return saver
    try {
      createLock.lock()
      saver = savers.get(rd.name)
      if (saver == null) {
        //初始化NoSQL数据库
        saver = new ResourceSaver(syncConfig, rd, version, zk, delegateFinder, idNoSQL)
        saver.start()
        savers.put(rd.name, saver)
        //增加nosql instance实例
        AddInstance(rd.name, saver.nosql.get)
      }
      return saver
    } finally {
      createLock.unlock()
    }
  }

  private[internal] def removeResourceSaver(resourceName: String) {
    RemoveInstance(resourceName)
    val saver = savers.remove(resourceName)
    if (saver != null) {
      saver.stop()
    }
  }

  private[internal] def destroyResourceSaver(resourceName: String) {
    RemoveInstance(resourceName)
    val saver = savers.remove(resourceName)
    if (saver != null) {
      saver.destroy()
    }
  }

  private[internal] def getSaver(resourceName: String): Option[ResourceSaver] = {
    val value = savers.get(resourceName)
    if (value == null) None else Some(value)
  }
}
