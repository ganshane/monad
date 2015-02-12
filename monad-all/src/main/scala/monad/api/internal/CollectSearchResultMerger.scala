// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.internal

import java.util.concurrent.CopyOnWriteArrayList

import com.google.gson.JsonArray
import monad.face.model.{NodeInfoSupport, ShardResult, ShardResultCollect}
import monad.rpc.internal.DefaultRpcResultMerger
import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}

/**
 * 合并搜索结果
 * @author jcai
 */
class CollectSearchResultMerger extends DefaultRpcResultMerger[ShardResult] with NodeInfoSupport {
  val collect = new ShardResultCollect
  private val list = new CopyOnWriteArrayList[ShardResult]()

  override def onResult(rpcServerLocation: RpcServerLocation, obj: ShardResult) {
    if (obj != null)
      list.add(obj)
  }


  /**
   * 执行和兴操作
   * @param remoteRequestParameter 远程调用的参数
   * @return 合并后的结果
   */
  override def merge(remoteRequestParameter: RemoteRequestParameter) = {
    collect.shardResults = list.toArray(Array[ShardResult]())

    collect
  }

  /**
   * 设置总节点数目
   * @param count 总节点数目
   */
  def setNodeCount(count: Int) {
    collect.nodesAll = count
  }

  /**
   * 设置成功的节点数目
   * @param count 成功的节点数
   */
  def setNodeSuccess(count: Int) {
    collect.nodesSuccess = count
  }


  def setNodeSuccess(servers: JsonArray) {
    collect.nodesSuccessInfo = servers
  }

  /**
   * 设置发生错误的节点数目
   * @param count 发生错误的节点数目
   */
  def setNodeError(count: Int) {
    collect.nodesError = count
  }
}
