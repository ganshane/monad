// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import monad.rpc.model.RpcServerLocation
import monad.rpc.services.RpcServerFinder
import monad.support.services.ZookeeperTemplate
import org.apache.tapestry5.json.JSONObject

/**
 * implements server finder
 * TODO offline or cache the server info
 */
class RpcServerFinderWithZk(zk: ZookeeperTemplate) extends RpcServerFinder {
  override def find(path: String): Option[RpcServerLocation] = {
    val strOpt = zk.getDataAsString(path)
    strOpt match {
      case Some(str) =>
        Some(RpcServerLocation.fromJSON(new JSONObject(str)))
      case None =>
        None
    }
  }
}
