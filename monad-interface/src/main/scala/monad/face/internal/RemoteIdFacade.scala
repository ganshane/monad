// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.internal

import monad.face.MonadFaceConstants
import monad.face.services.IdFacade
import monad.protocol.internal.InternalIdProto._
import monad.rpc.services.RpcClient

import scala.collection.mutable.ArrayBuffer

/**
 * 远程查询id相关信息的服务类
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-19
 */
class RemoteIdFacade(rpcClient:RpcClient) extends IdFacade{
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
  override def batchAddId(category: String, ids: Array[String]): Array[Int] = {
    val requestBuilder = BatchAddIdRequest.newBuilder()
    ids.foreach(requestBuilder.addLabel)
    requestBuilder.setCategory(IdCategory.valueOf(category))

    val future = rpcClient.writeMessageWithBlocking(MonadFaceConstants.MACHINE_ID,BatchAddIdRequest.cmd,requestBuilder.build())
    val response = future.get().getExtension(BatchAddIdResponse.cmd)
    val it = response.getOrdList.iterator()
    val buffer = new ArrayBuffer[Int]
    while (it.hasNext) buffer += it.next()
    buffer.toArray
  }

  override def putIfAbsent(category: String, label: String): Int = {
    val addIdRequest = AddIdRequest.newBuilder()
      .setCategory(IdCategory.valueOf(category))
      .setLabel(label)

    val future = rpcClient.writeMessageWithBlocking(MonadFaceConstants.MACHINE_ID,AddIdRequest.cmd,addIdRequest.build)
    if (future.get != null){
      val response = future.get().getExtension(AddIdResponse.cmd)
      response.getOrd
    }else MonadFaceConstants.UNKNOWN_ID_SEQ
  }
}
