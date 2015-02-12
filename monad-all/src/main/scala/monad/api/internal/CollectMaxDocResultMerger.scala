// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.internal

import java.util.concurrent.atomic.AtomicInteger

import monad.rpc.internal.DefaultRpcResultMerger
import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}

/**
 * 合并搜索到结果的最大值
 * @author jcai
 */
class CollectMaxDocResultMerger extends DefaultRpcResultMerger[Int] {
  private val i = new AtomicInteger()

  /**
   * 得到某一个节点的数据
   * @param obj 接受到一个对象
   */
  override def onResult(rpcServerLocation: RpcServerLocation, obj: Int) {
    i.addAndGet(obj)
  }

  /**
   * 执行和兴操作
   * @param remoteRequestParameter 远程调用的参数
   * @return 合并后的结果
   */
  override def merge(remoteRequestParameter: RemoteRequestParameter) = i.intValue()
}
