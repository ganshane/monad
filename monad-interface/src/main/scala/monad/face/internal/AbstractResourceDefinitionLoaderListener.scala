// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.face.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import monad.face.model.ResourceDefinition
import monad.face.services.{MonadFaceExceptionCode, ResourceDefinitionLoaderListener}
import monad.support.services.{MonadException, ServiceLifecycle, ServiceUtils}
import org.slf4j.LoggerFactory

/**
 * 抽象的资源定义加载监听器
 * @author jcai
 */
trait AbstractResourceDefinitionLoaderListener[T <: ServiceLifecycle]
  extends ResourceDefinitionLoaderListener {
  protected[internal] val objects = new ConcurrentHashMap[String, T]
  private val logger = LoggerFactory getLogger getClass
  private val locker = new ReentrantLock()

  def onResourceLoaded(rd: ResourceDefinition, version: Int) {
    //加载新的对象
    var obj = objects.get(rd.name)
    if (obj == null) {
      try {
        locker.lock()
        obj = objects.get(rd.name)
        if (obj == null) {
          //还是空，说明对象未创建
          //创建并且启动,先启动对象，避免对象还未启动，直接使用对象的情况发生
          obj = createObject(rd, version)
          if (obj == null) {
            logger.warn("fail to start object for {}", rd.name)
          } else {
            obj.start()

            //对象启动成功后，才进行管理
            objects.put(rd.name, obj)
          }
        }
      } catch {
        case e: MonadException =>
          logger.error(e.toString)
        case e: Throwable =>
          logger.error("Fail to start object " + obj, e)
      } finally {
        locker.unlock()
      }
    }
  }

  /**
   * 重新抽取的动作
   * @param resourceName
   */
  def onRemove(resourceName: String) {
    logger.debug("[{}] delete resource ...", resourceName)
    try {
      //加入锁避免多次删除某一个资源引起冲突
      locker.lock()
      val obj = objects.remove(resourceName)
      if (obj != null) {
        ServiceUtils.runInNoThrow(obj.shutdown())
        afterObjectRemoved(obj)
      }
    } finally {
      locker.unlock()
    }
  }

  protected def afterObjectRemoved(obj: T) {
  }

  protected def createObject(rd: ResourceDefinition, version: Int): T

  def getObject(key: String): T = ServiceUtils.waitUntilObjectLive[T]("%s".format(key)) {
    objects.get(key)
  }

  def directGetObject(key: String): T = {
    val obj = objects.get(key)
    if (obj == null) {
      throw new MonadException("resource:" + key + " not found", MonadFaceExceptionCode.OBJECT_NOT_LIVE)
    }

    obj
  }

  def onResourceUnloaded(resourceKey: String) {
    val oldObj = objects.remove(resourceKey)
    if (oldObj != null) oldObj.shutdown()
  }
}
