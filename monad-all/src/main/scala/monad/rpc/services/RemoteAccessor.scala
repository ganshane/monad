// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.services

import java.util.concurrent.Future

import monad.rpc.model.RemoteRequestParameter
import monad.support.services.ServiceLifecycle

/**
 * 远程访问请求
 * @author jcai
 */
trait RemoteAccessor extends ServiceLifecycle {
  /**
   * 远程访问
   * @param remoteRequestParameter 远程访问的参数
   * @return 结果
   */
  def invoke(remoteRequestParameter: RemoteRequestParameter): Any

  /**
   * invokeRpc，返回一个rpcFuture
   * @param remoteRequestParameter 远程访问参数
   * @return Future
   */
  def invokeRpc(remoteRequestParameter: RemoteRequestParameter): Future[Any]
}
