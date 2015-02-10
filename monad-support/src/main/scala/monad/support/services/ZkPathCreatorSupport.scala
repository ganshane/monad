// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import monad.support.MonadSupportConstants
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.NodeExistsException

/**
 * zk path creator
 */
trait ZkPathCreatorSupport {
  this: ZkClientSupport =>
  def createPersistPathWithString(path: String, data: Option[String] = None) {
    createPersistPath(path, data.map(_.getBytes(MonadSupportConstants.UTF8_ENCODING)))
  }

  def createPersistPath(path: String, data: Option[Array[Byte]] = None) {
    internalCreatePath(path, data)
  }

  protected def internalCreatePath(path: String,
                                   data: Option[Array[Byte]] = None,
                                   createMode: CreateMode = CreateMode.PERSISTENT) {
    try {
      val creator = zkClient.create().
        creatingParentsIfNeeded().
        withMode(createMode)
      data match {
        case Some(arr) =>
          creator.forPath(path, arr)
        case None =>
          creator.forPath(path)
      }
    } catch {
      case e: NodeExistsException =>
        data match {
          case Some(arr) =>
            zkClient.setData().forPath(path, arr)
          case None =>
          //do nothing
        }
      case other: Throwable =>
        throw new IllegalStateException(other.getMessage, other)
    }
  }
}
