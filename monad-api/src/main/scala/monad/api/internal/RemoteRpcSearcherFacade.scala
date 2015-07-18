// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import monad.face.MonadFaceConstants
import monad.face.model.{IdShardResult, ShardResult}
import monad.face.services.RpcSearcherFacade
import monad.protocol.internal.InternalFindDocProto.{InternalFindDocRequest, InternalFindDocResponse}
import monad.protocol.internal.InternalIdProto.{GetIdLabelRequest, GetIdLabelResponse, IdCategory, IdSearchRequest}
import monad.protocol.internal.InternalMaxdocQueryProto.MaxdocQueryRequest
import monad.protocol.internal.InternalSearchProto.InternalSearchRequest
import monad.rpc.services.RpcClient

import scala.collection.mutable.ArrayBuffer

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
    val builder = InternalSearchRequest.newBuilder()
    builder.setResourceName(resourceName)
    builder.setQ(q)
    if (sort != null)
      builder.setSort(sort)

    builder.setTopN(topN)
    val future = rpcClient.writeMessageToMultiServer(MonadFaceConstants.MACHINE_NODES, ApiMessageFilter.createCollectSearchMerger(), InternalSearchRequest.cmd, builder.build())
    future.get()
  }

  /**
   * 搜索对象
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
   * @param serverId 服务器的Hash值
   * @param resourceName 资源名称
   * @param key 键值
   * @return 数据值
   */
  override def findObject(serverId: Short, resourceName: String, key: Int): Option[Array[Byte]] = {
    val builder = InternalFindDocRequest.newBuilder()
    builder.setId(key)
    builder.setResourceName(resourceName)
    val future = rpcClient.writeMessageWithBlocking(MonadFaceConstants.MACHINE_NODE_FORMAT.format(serverId), InternalFindDocRequest.cmd, builder.build)
    val resultCommand = future.get
    if (resultCommand == null)
      return None

    val findDocResponse = resultCommand.getExtension(InternalFindDocResponse.cmd)
    if (findDocResponse.hasJson)
      Some(findDocResponse.getJson.toByteArray)
    else
      None
  }

  override def maxDoc(resourceName: String): Long = {
    val builder = MaxdocQueryRequest.newBuilder()
    builder.setResourceName(resourceName)
    val future = rpcClient.writeMessageToMultiServer(MonadFaceConstants.MACHINE_NODES, ApiMessageFilter.createMaxdocMerger, MaxdocQueryRequest.cmd, builder.build())
    future.get()
  }

  /**
   * 通过服务器的ID和资源名称，以及id序列，来查找对象的ID值
   * @param idSeqs id序列
   * @return id的值
   */
  override def findObjectId(category:String,idSeqs: Array[Int]): Array[String] = {
    val requestBuilder = GetIdLabelRequest.newBuilder()
    idSeqs.foreach(requestBuilder.addOrd)
    requestBuilder.setCategory(IdCategory.valueOf(category))

    val future = rpcClient.writeMessageWithBlocking(MonadFaceConstants.MACHINE_ID,GetIdLabelRequest.cmd,requestBuilder.build())
    val response = future.get().getExtension(GetIdLabelResponse.cmd)
    val it = response.getLabelList.iterator()
    val buffer = new ArrayBuffer[String]
    while (it.hasNext) buffer += it.next()
    buffer.toArray
  }
}
