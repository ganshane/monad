// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.face.MonadFaceConstants
import stark.rpc.config.RpcBindSupport
import stark.rpc.model.RpcServerLocation
import stark.rpc.services.RpcServerListener
import stark.utils.services.ZookeeperTemplate

/**
 * sync rpc server listener
 */
class SyncRpcServerListener(zk: ZookeeperTemplate, rpc: RpcBindSupport) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.exposeRpcLocation(rpc.rpc)
    zk.createEphemeralPathWithStringData(
      MonadFaceConstants.MACHINE_SYNC,
      Some(rpcServerLocation.toJSON.toCompactString))
  }
}
