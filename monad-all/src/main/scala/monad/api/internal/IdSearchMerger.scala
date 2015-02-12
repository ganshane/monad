// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.internal

import java.util.concurrent.CopyOnWriteArrayList

import com.google.gson.JsonArray
import monad.face.model.{IdShardResult, IdShardResultCollect, NodeInfoSupport}
import monad.rpc.internal.DefaultRpcResultMerger
import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}

/**
 * 针对ID进行搜索，也就是分析
 * @author jcai
 */
class IdSearchMerger extends DefaultRpcResultMerger[IdShardResult] with NodeInfoSupport {
  private val r: IdShardResultCollect = new IdShardResultCollect
  private val list = new CopyOnWriteArrayList[IdShardResult]()

  /**
   * 得到某一个节点的数据
   * @param obj 接受到一个对象
   */
  override def onResult(rpcServerLocation: RpcServerLocation, obj: IdShardResult) {
    //处理为空的情况
    if (obj == null) return
    list.add(obj)
  }

  override def merge(remoteRequestParameter: RemoteRequestParameter) = {
    r.results = list.toArray(new Array[IdShardResult](list.size()))

    r
  }

  /**
   * 设置总节点数目
   * @param count 总节点数目
   */
  def setNodeCount(count: Int) {
    r.nodesAll = count
  }

  /**
   * 设置成功的节点数目
   * @param count 成功的节点数
   */
  def setNodeSuccess(count: Int) {
    r.nodesSuccess = count
  }


  def setNodeSuccess(servers: JsonArray) {
    r.nodesSuccessInfo = servers
  }

  /**
   * 设置发生错误的节点数目
   * @param count 发生错误的节点数目
   */
  def setNodeError(count: Int) {
    r.nodesError = count
  }
}
