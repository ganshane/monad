package monad.api.internal

import monad.face.model.IdSeqShardResult
import monad.rpc.internal.DefaultRpcResultMerger
import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}

/**
 * find id seq merger
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class FindIdSeqMerger extends DefaultRpcResultMerger[Option[IdSeqShardResult]] {
  @volatile
  private var r: Option[IdSeqShardResult] = None

  override def onResult(rpcServerLocation: RpcServerLocation, obj: Option[IdSeqShardResult]) {
    if (obj.isDefined) r = obj
  }

  override def merge(remoteRequestParameter: RemoteRequestParameter) = {
    r
  }
}
