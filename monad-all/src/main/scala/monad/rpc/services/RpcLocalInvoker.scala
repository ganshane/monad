// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.services

/**
 * 调用本地的服务类
 * @author jcai
 */
trait RpcLocalInvoker {
  /**
   * invoke接口
   * @param interface 接口方法
   * @param methodId 方法名称
   * @param args 方法参数
   * @return 方法执行的结果
   */
  def invoke(interface: String, methodId: Int, args: Any*): Object

  /**
   * invoke接口
   * @param interface 接口方法
   * @param methodId 方法名称
   * @param args 方法参数
   * @return 方法执行的结果
   */
  def invoke(interface: String, methodId: Int, args: Array[Any]): Object
}
