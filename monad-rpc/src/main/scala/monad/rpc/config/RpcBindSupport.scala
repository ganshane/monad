// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * rpc server config
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RpcBindSupport")
trait RpcBindSupport {
  @XmlElement(name = "rpc")
  var rpc: RpcBind = new RpcBind
}

class RpcBind {
  @XmlElement(name = "bind")
  var bind: String = _
  @XmlElement(name = "io_thread")
  var ioThread: Int = 1
  @XmlElement(name = "worker_thread")
  var workerThread: Int = 8
}