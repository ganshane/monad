// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import com.google.protobuf.ByteString
import monad.core.MonadCoreConstants
import monad.face.MonadFaceConstants
import monad.face.model.{IdShardResult, ShardResult}
import monad.face.services.{GroupServerApi, RpcSearcherFacade}
import monad.protocol.internal.InternalIdProto._
import monad.protocol.internal.InternalMaxdocQueryProto.MaxdocQueryRequest
import org.apache.hadoop.hbase.{HConstants, HBaseConfiguration}
import org.apache.hadoop.hbase.client.Result
import roar.api.services.RoarClient
import roar.protocol.generated.RoarProtos.SearchResponse
import stark.rpc.services.RpcClient

/**
 * implements rpc searcher facade
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-26
 */
class RemoteRpcSearcherFacade(rpcClient: RpcClient,groupApi:GroupServerApi) extends RpcSearcherFacade {
  private val conf = HBaseConfiguration.create()
  conf.set(HConstants.ZOOKEEPER_QUORUM, groupApi.GetCloudAddress)
  conf.set(HConstants.HBASE_CLIENT_RETRIES_NUMBER,"3")
  conf.set(HConstants.HBASE_RPC_TIMEOUT_KEY,"30000")
//  conf.set(HConstants.HBASE_CLIENT_IPC_POOL_SIZE)
  conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT,MonadCoreConstants.GROUPS_PATH + "/" + groupApi.GetSelfGroupConfig.id)

  private val roarClient = new RoarClient(conf)
  /**
   * search index with index name and keyword
   */
  override def collectSearch(resourceName: String, q: String, sort: String, offset: Int,size:Int): SearchResponse = {
    /*
    val builder = InternalSearchRequest.newBuilder()
    builder.setResourceName(resourceName)
    builder.setQ(q)
    if (sort != null)
      builder.setSort(sort)

    builder.setTopN(topN)
    val future = rpcClient.writeMessageToMultiServer(MonadFaceConstants.MACHINE_NODES, ApiMessageFilter.createCollectSearchMerger(), InternalSearchRequest.cmd, builder.build())
    future.get()
    */
    roarClient.search(resourceName,q,Option(sort),offset,size)
  }

  /**
   * 搜索对象
    *
    * @param resourceName 资源名称
   * @param q 搜索条件
   * @return 搜索比中结果
   */
  override def searchObjectId(resourceName: String, q: String): IdShardResult = {
    val builder = IdSearchRequest.newBuilder()
    builder.setResourceName(resourceName)
    builder.setQ(q)
    val future = rpcClient.writeMessageToMultiServer(MonadFaceConstants.MACHINE_NODES, ApiMessageFilter.createIdSearchMerger(), IdSearchRequest.cmd, builder.build())
    future.get()
  }

  override def facetSearch(resourceName: String, q: String, field: String, upper: Int, lower: Int): ShardResult = {
    throw new UnsupportedOperationException
  }

  /**
   * 查找对象的详细信息
    *
    * @param serverId 服务器的Hash值
   * @param resourceName 资源名称
   * @param key 键值
   * @return 数据值
   */
  override def findObject(serverId: Short, resourceName: String, key: ByteString): Option[Result] = {
    roarClient.findRow(resourceName,key.toByteArray)
  }

  override def maxDoc(resourceName: String): Long = {
    val builder = MaxdocQueryRequest.newBuilder()
    builder.setResourceName(resourceName)
    val future = rpcClient.writeMessageToMultiServer(MonadFaceConstants.MACHINE_NODES, ApiMessageFilter.createMaxdocMerger, MaxdocQueryRequest.cmd, builder.build())
    future.get()
  }

}
