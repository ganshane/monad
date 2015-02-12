// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.internal

import monad.rpc.model.{RemoteRequestParameter, RpcServerLocation}
import monad.rpc.services.RpcResultMerger
import monad.support.services.MonadException
import org.slf4j.LoggerFactory

/**
 * default rpc result merger
 * @author jcai
 */
class DefaultRpcResultMerger[T] extends RpcResultMerger[T] {
  private val logger = LoggerFactory getLogger getClass
  @volatile
  private var result: T = _

  def onResult(rpcServerLocation: RpcServerLocation, obj: T) {
    result = obj
  }

  def merge(remoteRequestParameter: RemoteRequestParameter) = result

  /**
   * 当发生异常的时候进行处理
   * @param ex 异常消息
   */
  def onException(rpcServerLocation: RpcServerLocation, ex: Throwable) {
    ex match {
      case e: MonadException =>
        logger.error("[" + rpcServerLocation + "] {}", ex.toString)
      case other =>
        logger.error("[" + rpcServerLocation + "] " + other.getMessage, other)
    }
  }
}
