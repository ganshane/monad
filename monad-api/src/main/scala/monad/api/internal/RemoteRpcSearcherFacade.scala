// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import com.google.protobuf.ByteString
import monad.face.MonadFaceConstants
import monad.face.internal.HBaseRegionMapping
import monad.face.model.{IdShardResult, IdShardResultCollect, ShardResult}
import monad.face.services.RpcSearcherFacade
import monad.protocol.internal.InternalMaxdocQueryProto.MaxdocQueryRequest
import org.apache.hadoop.hbase.client.Result
import roar.api.services.RoarClient
import roar.protocol.generated.RoarProtos.SearchResponse
import stark.rpc.services.RpcClient
import stark.utils.services.LoggerSupport

/**
 * implements rpc searcher facade
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-26
 */
class RemoteRpcSearcherFacade(rpcClient: RpcClient,roarClient: RoarClient) extends RpcSearcherFacade with LoggerSupport{

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
    val result = roarClient.idSearch(resourceName,"object_id",q)
    val collect = new IdShardResultCollect
    collect.results = result.filter(_.hasRegionId).map{response=>
      val shard = new IdShardResult
      shard.data = response.getData

      val regionKey = response.getRegionId
      val regionId = HBaseRegionMapping.getRegionMappingId(regionKey)
      shard.region = regionId
      /*
      val bitSet = MonadSparseFixedBitSet.deserialize(new ByteArrayInputStream(shard.data.toByteArray))
      info("region:{} regionId {} length:{} nonzero:{} cardinality:{} ",
        regionKey,
        regionId,bitSet.length(),bitSet.nonZeroLongCount,bitSet.cardinality());
       */

      shard
    }

    collect
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
