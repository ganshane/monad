package monad.api.internal

import monad.face.MonadFaceConstants
import monad.face.model.ShardResult
import monad.face.services.RpcSearcherFacade
import monad.protocol.internal.InternalMaxdocQueryProto.MaxdocQueryRequest
import monad.rpc.services.RpcClient

/**
 * implements rpc searcher facade
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-26
 */
class RemoteRpcSearcherFacade(rpcClient: RpcClient) extends RpcSearcherFacade {
  /**
   * search index with index name and keyword
   */
  override def collectSearch(resourceName: String, q: String, sort: String, topN: Int): ShardResult = {
    throw new UnsupportedOperationException
  }

  override def facetSearch(resourceName: String, q: String, field: String, upper: Int, lower: Int): ShardResult = {
    throw new UnsupportedOperationException
  }

  override def collectSearch2(resourceName: String, q: String, sort: String, topN: Int): ShardResult = {
    throw new UnsupportedOperationException
  }

  /**
   * 查找对象的详细信息
   * @param serverId 服务器的Hash值
   * @param resourceName 资源名称
   * @param key 键值
   * @return 数据值
   */
  override def findObject(serverId: Short, resourceName: String, key: Array[Byte]): Option[Array[Byte]] = {
    throw new UnsupportedOperationException
  }

  override def maxDoc(resourceName: String): Long = {
    val builder = MaxdocQueryRequest.newBuilder()
    builder.setResourceName(resourceName)
    val future = rpcClient.writeMessageToMultiServer(MonadFaceConstants.MACHINE_NODE, ApiMessageFilter.createMaxdocMerger, MaxdocQueryRequest.cmd, builder.build())
    future.get()
  }
}
