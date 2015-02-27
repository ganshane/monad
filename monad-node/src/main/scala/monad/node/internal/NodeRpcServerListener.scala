package monad.node.internal

import monad.face.MonadFaceConstants
import monad.rpc.config.RpcBindSupport
import monad.rpc.model.RpcServerLocation
import monad.rpc.services.RpcServerListener
import monad.support.services.{SequenceEphemeralMode, ZookeeperTemplate}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-27
 */
class NodeRpcServerListener(zk: ZookeeperTemplate, rpc: RpcBindSupport) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.fromBindString(rpc.rpc.bind)
    zk.createEphemeralPathWithStringData(
      MonadFaceConstants.MACHINE_NODE + "/s",
      Some(rpcServerLocation.toJSON.toCompactString),
      SequenceEphemeralMode
    )
  }
}
