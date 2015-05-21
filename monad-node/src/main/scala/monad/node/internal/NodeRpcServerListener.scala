// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import monad.core.config.PartitionIdSupport
import monad.face.MonadFaceConstants
import monad.rpc.config.RpcBindSupport
import monad.rpc.model.RpcServerLocation
import monad.rpc.services.RpcServerListener
import monad.support.services.ZookeeperTemplate

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-27
 */
class NodeRpcServerListener(zk: ZookeeperTemplate, rpc: RpcBindSupport with PartitionIdSupport) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.exposeRpcLocation(rpc.rpc)
    zk.createEphemeralPathWithStringData(
      MonadFaceConstants.MACHINE_NODE_FORMAT.format(rpc.partitionId),
      Some(rpcServerLocation.toJSON.toCompactString))
  }
}
