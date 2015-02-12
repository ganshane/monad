// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.services

import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}

/**
 * 用来针对查询结果的合并操作
 * @author jcai
 */
trait RpcResultMerger[T] {
  /**
   * 得到某一个节点的数据
   * @param rpcServerLocation 远程服务的位置信息
   * @param obj 接受到一个对象
   */
  def onResult(rpcServerLocation: RpcServerLocation, obj: T)

  /**
   * 当发生异常的时候进行处理
   * @param rpcServerLocation 远程服务的位置信息
   * @param ex 异常消息
   */
  def onException(rpcServerLocation: RpcServerLocation, ex: Throwable)

  /**
   * 执行和兴操作
   * @param remoteRequestParameter 远程调用的参数
   * @return 合并后的结果
   */
  def merge(remoteRequestParameter: RemoteRequestParameter): T
}
