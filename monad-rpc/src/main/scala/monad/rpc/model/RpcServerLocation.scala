// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.model

import monad.support.services.MonadUtils
import org.apache.tapestry5.json.JSONObject


/**
 * rpc server location.
 */
class RpcServerLocation(val host: String, val port: Int, val weight: Int = 1, val partition: Short = 0) {
  var channelCount: Int = 5
  /** 连接超时的时间数 **/
  var connectTimeoutInMillis: Int = 100

  override def equals(p1: Any): Boolean = {
    if (p1 == null || !p1.isInstanceOf[RpcServerLocation]) return false
    val tmp = p1.asInstanceOf[RpcServerLocation]
    tmp.host == host && tmp.port == tmp.port
  }

  override def hashCode(): Int = {
    (host + port).hashCode
  }

  override def toString = {
    val sb = new StringBuilder
    sb.append("host:").append(host).append(",")
    sb.append("port:").append(port).append(",")
    sb.append("weight:").append(weight).append(",")
    sb.append("partition:").append(partition.toString)
    sb.toString()
  }

  def toJSON = {
    val json = new JSONObject()
    json.put("host", host)
    json.put("port", port)
    json.put("weight", weight)
    json.put("partition", partition)

    json
  }
}

object RpcServerLocation {
  def fromJSON(json: JSONObject) = {
    val host = json.getString("host")
    val port = json.getInt("port")
    val weight = if (json.has("weight")) json.getInt("weight") else 1
    val region: Short = if (json.has("partition")) json.getInt("partition").toShort else 0
    new RpcServerLocation(host, port, weight, region)
  }

  def fromBindString(bind: String) = {
    val tuple = MonadUtils.parseBind(bind)
    new RpcServerLocation(tuple._1, tuple._2.toInt)
  }
}
