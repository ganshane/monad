// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.model

import monad.rpc.config.RpcBind
import monad.support.services.MonadUtils
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.json.JSONObject


/**
 * rpc server location.
 */
class RpcServerLocation(val host: String, val port: Int) {
  @deprecated(message = "will be removed",since="5.0.16")
  def this(host:String,port:Int,weight:Int,partition:Int){
    this(host,port)
  }
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
    sb.toString()
  }

  def toJSON = {
    val json = new JSONObject()
    json.put("host", host)
    json.put("port", port)

    json
  }
}

object RpcServerLocation {
  def fromJSON(json: JSONObject) = {
    val host = json.getString("host")
    val port = json.getInt("port")
    new RpcServerLocation(host, port)
  }

  def exposeRpcLocation(bind: RpcBind) = {
    var exposeAddress = bind.expose
    if (InternalUtils.isBlank(exposeAddress)) {
      exposeAddress = bind.bind
    }
    val tuple = MonadUtils.parseBind(exposeAddress)
    new RpcServerLocation(tuple._1, tuple._2.toInt)
  }
}
