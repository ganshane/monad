// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
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
