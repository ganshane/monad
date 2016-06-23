// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.internal

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{ConcurrentHashMap, Executors, ThreadFactory, TimeUnit}
import javax.annotation.PostConstruct

import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.{EventFactory, EventTranslator}
import monad.face.CloudPathConstants
import monad.face.model.ResourceEvent.ResourceEventType
import monad.face.model.{ResourceDefinition, ResourceEvent}
import monad.face.services.{GroupZookeeperTemplate, ResourceDefinitionLoader, ResourceDefinitionLoaderListener}
import stark.utils.StarkUtilsConstants
import stark.utils.services.{ChildrenDataWatcher, NodeDataWatcher, XmlLoader}
import org.apache.tapestry5.ioc.services.{ParallelExecutor, RegistryShutdownHub}
import org.apache.tapestry5.services.Core
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

/**
 * 针对资源的监控
 * @author jcai
 */
//@EagerLoad
class ResourcesWatcher(zk: GroupZookeeperTemplate,
                       @Core listener: ResourceDefinitionLoaderListener,
                       parallelExecutor: ParallelExecutor) extends ResourceDefinitionLoader {
  private val resources = new ConcurrentHashMap[String, ResourceDefinition]()
  private val logger = LoggerFactory getLogger getClass
  private val locker = new ReentrantLock()
  //针对数据的检测
  private val EVENT_FACTORY = new EventFactory[ResourceEvent] {
    def newInstance() = new ResourceEvent()
  }
  private val buffer = 1 << 2
  //单线程异步处理资源事件
  private val disruptor = new Disruptor[ResourceEvent](EVENT_FACTORY, buffer, Executors.newFixedThreadPool(1, new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(true)
      t
    }
  }))

  private[internal] var children = Seq[String]()
  private var hasClosed = false

  /**
   * 启动对象实例
   */
  @PostConstruct
  def start(hub: RegistryShutdownHub) {
    disruptor.handleEventsWith(new ResourceEventHandler(listener, zk))
    disruptor.start()
    zk.watchChildren(CloudPathConstants.RESOURCES_PATH,
      new ChildrenDataWatcher {
        def handleDataChanged(newChildren: Seq[String]) {
          try {
            locker.lock()
            //增加的节点
            newChildren.diff(children).foreach(x => watch(x, CloudPathConstants.RESOURCE_PATH_FORMAT.format(x)))
            children = newChildren
            logger.info("latest resources:[{}]", children)
          } finally {
            locker.unlock()
          }
        }
      })

    hub.addRegistryWillShutdownListener(new Runnable {
      override def run(): Unit = shutdown()
    })
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    if (hasClosed)
      return
    logger.info("closing resource definition loader...")
    resources.keySet().foreach(listener.onResourceUnloaded)
    resources.clear()
    hasClosed = true
    try {
      disruptor.shutdown(2, TimeUnit.SECONDS)
    } catch {
      case NonFatal(e) =>
        disruptor.halt()
    }
  }

  private def watch(key: String, path: String) {
    logger.debug("[{}] begin to watch resource path {}", key, path)
    zk.watchNodeData(path,
      new NodeDataWatcher {
        def handleNodeDeleted() {
          logger.debug("[{}] watch delete path {}", key, path)
          removeResource(key)
        }

        def handleDataChanged(data: Option[Array[Byte]]) {
          if (data.isDefined) {
            val x = data.get
            logger.debug("[{}] watched resource path {}", key, path)
            val rd = XmlLoader.parseXML[ResourceDefinition](new String(x, StarkUtilsConstants.UTF8_ENCODING))
            val resyncStat = zk.stat(CloudPathConstants.RESOURCE_RESYNC_PATH_FORMAT.format(key))
            val stat = zk.stat(path).get
            resyncStat match {
              case Some(y) =>
                resync(rd, stat.getVersion)
              case None =>
                pushEvent(rd, ResourceEvent.Start(stat.getVersion))
            }
            resources.put(key, rd)
          }
        }
      })
  }

  def resync(rd: ResourceDefinition, version: Int) {
    removeResource(rd.name)
    pushEvent(rd, ResourceEvent.Start(version))
  }

  def removeResource(key: String) = {
    val obj = resources.remove(key)
    if (obj != null) {
      pushEvent(obj, ResourceEvent.Remove)
    }
    obj
  }

  private def pushEvent(resource: ResourceDefinition, resourceEventType: ResourceEventType) {
    disruptor.publishEvent(new EventTranslator[ResourceEvent] {
      def translateTo(event: ResourceEvent, sequence: Long) {
        event.resource = resource
        event.eventType = resourceEventType
      }
    })
  }

  def getResourceDefinitions = resources.values().iterator()

  def getResourceDefinition(name: String) = {
    val v = resources.get(name)
    if (v == null) None else Some(v)
  }
}
