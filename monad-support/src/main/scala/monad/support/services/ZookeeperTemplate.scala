// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent._

import org.apache.tapestry5.ioc.annotations.EagerLoad
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooKeeper}

/**
 * zookeeper template
 * @author jcai
 */
@EagerLoad
class ZookeeperTemplate(val address: String,
                        val basePath: Option[String] = None,
                        val sessionTimeout: Int = 6000)
  extends ZkClientSupport
  with LoggerSupport
  with ZkDeletePathSupport
  with ZkPathCreatorSupport
  with ZkNodeDataSupport
  with RunInNoExceptionThrown
  with ZkChildrenSupport
  with ZkEphemeralPathSupport {

  //only for test session expired
  private[monad] def buildAnotherSession = {
    val zookeeper = zkClient.getZookeeperClient.getZooKeeper
    val (sessionId, sessionPasswd) = (zookeeper.getSessionId, zookeeper.getSessionPasswd)
    val self = new CountDownLatch(1)
    val zk = new ZooKeeper(address, sessionTimeout, new Watcher {
      def process(event: WatchedEvent) {
        event.getState match {
          case KeeperState.SyncConnected =>
            //monitor
            debug("Connected to cloud server")
            self.countDown()
          case other =>
            debug("other state:{}", event.getState)
        }

      }
    }, sessionId, sessionPasswd)
    self.await()

    zk
  }
}

class Node(val path: String, val data: Option[Array[Byte]], val createMode: CreateMode) extends Comparable[Node] {
  override def equals(p1: Any) = {
    val p = p1.asInstanceOf[Node]
    p.path == path
  }

  def compareTo(p1: Node) = p1.path.compareTo(path)
}

