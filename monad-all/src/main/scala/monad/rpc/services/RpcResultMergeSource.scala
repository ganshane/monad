// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.services

/**
 * 支持的merge集合
 * @author jcai
 */
trait RpcResultMergeSource {
  /**
   * 得到merge的集合类
   * @param name merger名称
   * @tparam T merge操作的类型
   * @return merger对象
   */
  def getMerger[T](name: String): RpcResultMerger[T]
}
