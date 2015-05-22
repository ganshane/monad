// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArraySet}

import org.apache.curator.framework.api.CuratorWatcher
import org.apache.zookeeper.KeeperException.NoNodeException
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher.Event.EventType

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

/**
 * zk children support
 */
trait ZkChildrenSupport {
  this: ZkClientSupport
    with ZkPathCreatorSupport
    with ZkDeletePathSupport
    with ZkNodeDataSupport
    with RunInNoExceptionThrown
    with LoggerSupport =>

  /**
   * 子节点观察的监听者,当父节点本身被删除，然后被创建，那么此watcher仍然有效
   */
  private final val childrenWatcher = new ConcurrentHashMap[String, ChildrenWatcherList]()
  private final val failedChildrenWatcher = new CopyOnWriteArraySet[String]()
  private val childrenCuratorWatcher = new CuratorWatcher {
    def process(event: WatchedEvent) {
      if (event.getPath == null) return
      val selfWatchers = childrenWatcher.get(event.getPath)
      if (selfWatchers == null) return
      event.getType match {
        case EventType.NodeChildrenChanged | EventType.NodeCreated =>
          val data = internalWatchChildren(event.getPath, this)
          selfWatchers.watchers.foreach(x => runInNotExceptionThrown {
            x.handleDataChanged(data)
          })
        case EventType.NodeDeleted =>
          zkClient.checkExists().usingWatcher(this).forPath(event.getPath)
        case other =>
          debug("other event:{}", other)
      }
    }
  }

  def deleteRecursive(path: String) {
    val children = getChildren(path)
    for (node <- children) {
      deleteRecursive(path + '/' + node)
    }
    delete(path)
  }

  def watchChildren(path: String, watcher: ChildrenDataWatcher) {
    var isHandleData = false
    var watchers = childrenWatcher.get(path)
    try {
      if (watchers == null) {
        val arrayList = ChildrenWatcherList(childrenCuratorWatcher, new CopyOnWriteArraySet[ChildrenDataWatcher]())
        watchers = childrenWatcher.putIfAbsent(path, arrayList)
        if (watchers == null) {
          //说明首次添加，需要进行watcher
          watchers = arrayList
          //如果不存在此节点
          val nodeStat = stat(path)
          isHandleData = true
          nodeStat match {
            case Some(s) => //存在此节点
              //val seq = zkClient.getChildren.usingWatcher(arrayList.internalWatcher).forPath(path).toSeq
              val seq = internalWatchChildren(path, childrenCuratorWatcher)
              runInNotExceptionThrown {
                watcher.handleDataChanged(seq)
              }
            case None =>
              //check exists
              zkClient.checkExists().usingWatcher(arrayList.internalWatcher).forPath(path)
              runInNotExceptionThrown {
                watcher.handleDataChanged(Seq[String]())
              }
          }
        }
      }
      if (!isHandleData)
        runInNotExceptionThrown {
          watcher.handleDataChanged(getChildren(path))
        }
    } catch {
      case NonFatal(e) =>
        failedChildrenWatcher.add(path)
        error("fail to watch childre,will retry " + e.getMessage)
    }
    finally {
      if (watchers != null)
        watchers.watchers.add(watcher)
    }
  }

  def getChildren(path: String): Seq[String] = {
    try {
      zkClient.getChildren.forPath(path).toSeq
    } catch {
      case ex: NoNodeException =>
        Seq[String]()
    }
  }

  private def internalWatchChildren(path: String, curatorWatcher: CuratorWatcher): Seq[String] = {
    try {
      failedChildrenWatcher.remove(path)
      val data = zkClient.getChildren.usingWatcher(curatorWatcher).forPath(path)
      data.toSeq
    } catch {
      case NonFatal(e) =>
        warn("fail to watch node data,will retry,msg:{}", e.getMessage)
        failedChildrenWatcher.add(path)
        null
    }
  }

  protected def retryFailedChildrenWatcher() {
    debug("retry to watch children")
    val it = failedChildrenWatcher.iterator()
    while (it.hasNext) {
      val path = it.next()
      val watcherList = childrenWatcher.get(path)
      val data = internalWatchChildren(path, watcherList.internalWatcher)
      //针对每个watcher调用数据，进行执行
      if (data != null)
        watcherList.watchers.foreach(x => runInNotExceptionThrown {
          x.handleDataChanged(data)
        })
    }
  }

  protected def rewatchChildren() {
    //对子节点监测
    childrenWatcher.foreach {
      case (k, v) =>
        val data = internalWatchChildren(k, v.internalWatcher)
        //针对每个watcher调用数据，进行执行
        if (data != null)
          v.watchers.foreach(x => runInNotExceptionThrown {
            x.handleDataChanged(data)
          })

    }
  }

  //过滤掉针对children的重复监听
  private case class ChildrenWatcherList(internalWatcher: CuratorWatcher, watchers: CopyOnWriteArraySet[ChildrenDataWatcher])

}

/**
 * 针对子节点数据的查看器
 */
trait ChildrenDataWatcher {
  /**
   * 针对数据发变化的处理
   * @param data 子节点数据
   */
  def handleDataChanged(data: Seq[String])
}
