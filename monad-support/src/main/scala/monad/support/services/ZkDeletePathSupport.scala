// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import org.apache.zookeeper.KeeperException.NoNodeException

/**
 * zk delete path support
 */
trait ZkDeletePathSupport {
  this: ZkClientSupport =>
  def delete(path: String) {
    try {
      zkClient.delete().guaranteed().forPath(path)
    } catch {
      case e: NoNodeException => //do nothing
    }
  }
}
