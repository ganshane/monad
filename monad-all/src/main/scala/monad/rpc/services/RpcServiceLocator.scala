// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.services

import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}

/**
 * rpc service locator
 * @author jcai
 */
trait RpcServiceLocator {
  /**
   * 发现一个服务器
   * @param remoteRequestParameter 远程请求参数
   * @return 远程服务器定义
   */
  def findServer(remoteRequestParameter: RemoteRequestParameter): Array[RpcServerLocation]
}
