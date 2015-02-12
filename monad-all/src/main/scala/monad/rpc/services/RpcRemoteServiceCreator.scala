// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.services

/**
 * 调用远端的invoker
 * @author jcai
 */
trait RpcRemoteServiceCreator {
  /**
   * 创建远端的服务
   * @param interface 接口
   * @tparam T 接口类型
   * @return 实例
   */
  def createRemoteInstance[T](interface: Class[T]): T
}
