// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.face.MonadFaceConstants
import monad.rpc.config.RpcBindSupport
import monad.rpc.model.RpcServerLocation
import monad.rpc.services.RpcServerListener
import monad.support.services.ZookeeperTemplate

/**
 * sync rpc server listener
 */
class SyncRpcServerListener(zk: ZookeeperTemplate, rpc: RpcBindSupport) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.fromBindString(rpc.rpc.bind)
    zk.createEphemeralPathWithStringData(
      MonadFaceConstants.MACHINE_SYNC,
      Some(rpcServerLocation.toJSON.toCompactString))
  }
}