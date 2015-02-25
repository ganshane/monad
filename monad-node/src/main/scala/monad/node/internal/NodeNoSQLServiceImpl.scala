// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import monad.core.config.PartitionIdSupport
import monad.core.internal.SlaveNoSQLServiceImplSupport
import monad.face.MonadFaceConstants
import monad.face.config.IndexConfigSupport
import monad.jni.services.gen.{NoSQLOptions, SlaveNoSQLSupport}
import monad.rpc.config.RpcBindSupport
import monad.rpc.model.RpcServerLocation
import monad.rpc.services.RpcServerListener
import monad.support.services.ZookeeperTemplate

/**
 * implements NodeService
 */
class NodeNoSQLServiceImpl(config: IndexConfigSupport)
  extends SlaveNoSQLServiceImplSupport(config.noSql) {

  override protected def createNoSQLInstance(path: String, noSQLOption: NoSQLOptions): SlaveNoSQLSupport = {
    new SlaveNoSQLSupport(path, noSQLOption)
  }
}

class NodeRpcServerListener(zk: ZookeeperTemplate, rpc: RpcBindSupport with PartitionIdSupport) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.fromBindString(rpc.rpc.bind)
    zk.createEphemeralPathWithStringData(
      MonadFaceConstants.MACHINE_NODE.format(rpc.partitionId),
      Some(rpcServerLocation.toJSON.toCompactString))
  }
}

