// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArrayList, CopyOnWriteArraySet}

import monad.support.MonadSupportConstants
import org.apache.curator.framework.api.CuratorWatcher
import org.apache.zookeeper.KeeperException.NoNodeException
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.data.Stat

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

/**
 * zookeeper node data support
 */
trait ZkNodeDataSupport {
  this: ZkClientSupport
    with ZkPathCreatorSupport
    with ZkDeletePathSupport
    with RunInNoExceptionThrown
    with LoggerSupport =>

  /**
   * 对所有对节点数据的监听者,
   * 节点被删除，节点数据的watcher将失效,
   */
  private final val nodeDataWatcher = new ConcurrentHashMap[String, NodeDataWatcherList]()
  private final val failedNodeDataWatcher = new CopyOnWriteArraySet[String]()
  private val nodeCuratorWatcher = new CuratorWatcher {
    def process(event: WatchedEvent) {
      if (event.getPath == null) return
      val selfWatchers = nodeDataWatcher.get(event.getPath)
      if (selfWatchers == null) return
      event.getType match {
        case EventType.NodeDataChanged =>
          val data = internalWatchNodeData(event.getPath, this)
          selfWatchers.watchers.foreach(x => runInNotExceptionThrown {
            x.handleDataChanged(data)
          })
        case EventType.NodeDeleted =>
          try {
            selfWatchers.watchers.foreach(x => runInNotExceptionThrown {
              x.handleNodeDeleted()
            })
          } finally {
            //节点已经删除，同时删除此节点相关的watcher
            nodeDataWatcher.remove(event.getPath)
          }
        case other =>
          debug("other event:{}", other)
      }
    }
  }

  /**
   * 得到节点内容作为一个字符串
   * @param path 节点路径
   * @param stat 节点的stat信息,作为返回信息
   * @return 作为String返回节点的数据
   */
  def getDataAsString(path: String, stat: Option[Stat] = None): Option[String] = {
    getData(path, stat) match {
      case Some(arr) =>
        Some(new String(arr, MonadSupportConstants.UTF8_ENCODING))
      case None =>
        None
    }
  }

  /**
   * 把字符串设置到某一节点
   * @param path 节点路径
   * @param data 数据
   * @param stat 节点的状态信息
   * @param encoding 字符串的编码格式
   */
  def setStringData(path: String, data: String, stat: Option[Stat] = None,
                    encoding: String = MonadSupportConstants.UTF8_ENCODING) {
    setData(path, data.getBytes(encoding), stat)
  }

  /**
   * 设置某一节点的数据
   * @param path 节点路径
   * @param data 节点数据
   * @param stat 节点的状态
   */
  def setData(path: String, data: Array[Byte], stat: Option[Stat] = None) {
    stat match {
      case Some(s) =>
        zkClient.setData().withVersion(s.getVersion).forPath(path, data)
      case _ =>
        zkClient.setData().forPath(path, data)
    }
  }

  /**
   * 得到某一节点信息
   * @param path 节点路径
   * @return 节点信息
   */
  def stat(path: String): Option[Stat] = {
    val data = zkClient.checkExists().forPath(path)
    if (data == null) None else Some(data)
  }

  /**
   * watch某一节点数据
   * @param path 节点路径
   * @param watcher 节点watcher
   */
  def watchNodeData(path: String,
                    watcher: NodeDataWatcher) {
    var isHandleData = false
    var watchers = nodeDataWatcher.get(path)
    if (watchers == null) {
      val arrayList = NodeDataWatcherList(nodeCuratorWatcher, new CopyOnWriteArrayList[NodeDataWatcher]())

      watchers = nodeDataWatcher.putIfAbsent(path, arrayList)
      if (watchers == null) {
        //说明首次添加，需要进行watcher
        watchers = arrayList
        //通过watcher之后的值进行回调
        val data = internalWatchNodeData(path, nodeCuratorWatcher)
        isHandleData = true
        runInNotExceptionThrown {
          watcher.handleDataChanged(data)
        }
      }
    }
    if (!isHandleData) {
      runInNotExceptionThrown {
        watcher.handleDataChanged(getData(path))
      }
    }
    watchers.watchers.add(watcher)
  }

  /**
   * 获取节点数据
   * @param path 节点的路径
   * @param stat 节点的stat状态信息,作为返回值返回
   * @return 节点数据
   */
  def getData(path: String, stat: Option[Stat] = None): Option[Array[Byte]] = {
    try {
      val data =
        stat match {
          case Some(s) =>
            zkClient.getData.storingStatIn(s).forPath(path)
          case _ =>
            zkClient.getData.forPath(path)
        }
      if (data == null || data.length == 0) None else Some(data)
    } catch {
      case e: NoNodeException =>
        warn("no node for path:{}", path)
        None
    }
  }

  protected def rewatchNodeData() {
    //针对节点数据的观察器
    nodeDataWatcher.foreach {
      case (k, v) =>
        val data = internalWatchNodeData(k, v.internalWatcher)
        //针对各个watcher调用数据
        if (data.isDefined)
          v.watchers.foreach(x => runInNotExceptionThrown {
            x.handleDataChanged(data)
          })
    }
  }

  private def internalWatchNodeData(path: String,
                                    curatorWatcher: CuratorWatcher): Option[Array[Byte]] = {
    try {
      failedNodeDataWatcher.remove(path)
      val data = zkClient.getData.usingWatcher(curatorWatcher).forPath(path)
      if (data == null || data.length == 0) None else Some(data)
    } catch {
      case NonFatal(e) =>
        warn("fail to watch node data,will retry,msg:{}", e.getMessage)
        failedNodeDataWatcher.add(path)
        None
    }
  }

  protected def retryFailedWatchNodeData() {
    debug("retry to watch node data")
    val it = failedNodeDataWatcher.iterator()
    while (it.hasNext) {
      val path = it.next()
      val watcherList = nodeDataWatcher.get(path)
      val data = internalWatchNodeData(path, watcherList.internalWatcher)
      //针对各个watcher调用数据
      if (data.isDefined)
        watcherList.watchers.foreach(x => runInNotExceptionThrown {
          x.handleDataChanged(data)
        })
    }
  }

  private case class NodeDataWatcherList(internalWatcher: CuratorWatcher, watchers: CopyOnWriteArrayList[NodeDataWatcher])

}

/**
 * 针对节点数据的查看器
 */
trait NodeDataWatcher {
  /**
   * 针对数据发生变化的处理
   * @param data 变化后的数据
   */
  def handleDataChanged(data: Option[Array[Byte]])

  /**
   * 针对节点删除的处理
   */
  def handleNodeDeleted()
}
