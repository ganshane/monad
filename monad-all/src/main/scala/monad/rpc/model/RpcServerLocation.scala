// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.model

import java.io.Closeable

import monad.rpc.services.RpcResultMerger

/**
 * rpc server location
 * @author jcai
 */
class RpcServerLocation(val host: String, val port: Int, val weight: Int = 1, val region: Short = 0) extends Closeable {
  def close() = {
  }

  def invoke(remoteRequestParameter: RemoteRequestParameter, merger: RpcResultMerger[Any]) = {
  }

  override def toString = {
    val sb = new StringBuilder
    sb.append("host:").append(host).append(",")
    sb.append("port:").append(port).append(",")
    sb.append("weight:").append(weight).append(",")
    sb.append("region:").append(region.toString)
    sb.toString()
  }

  override def equals(p1: Any): Boolean = {
    if (p1 == null || !p1.isInstanceOf[RpcServerLocation]) return false
    val tmp = p1.asInstanceOf[RpcServerLocation]

    tmp.host == host && tmp.port == tmp.port
  }
}
