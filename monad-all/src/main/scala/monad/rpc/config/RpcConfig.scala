// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.config

import java.util
import javax.xml.bind.annotation._
import javax.xml.bind.annotation.adapters.{XmlAdapter, XmlJavaTypeAdapter}

import monad.core.config.ServerIdSupport

@XmlAccessorType(XmlAccessType.FIELD)
class RpcService {
  @XmlAttribute(name = "name")
  var name: String = _
  @XmlAttribute(name = "weight")
  var weight: Int = 1
}

class ServiceMapAdapter extends XmlAdapter[ServiceMapType, util.Map[String, RpcService]] {
  def unmarshal(p1: ServiceMapType) = {
    val map = new util.HashMap[String, RpcService]
    val it = p1.services.iterator()
    while (it.hasNext) {
      val s = it.next()
      map.put(s.name, s)
    }
    map
  }

  def marshal(p1: util.Map[String, RpcService]) = {
    throw new UnsupportedOperationException
  }
}

@XmlType
class ServiceMapType {
  @XmlElement(name = "service")
  var services = new util.ArrayList[RpcService]()
}

@XmlRootElement(name = "rpc")
@XmlAccessorType(XmlAccessType.FIELD)
class MonadRpcConfig extends ServerIdSupport {
  @XmlElement(name = "default_weight")
  var weight: Int = 1
  @XmlElement(name = "host")
  var host: String = _
  @XmlElement(name = "port")
  var port: Int = 5555
  @XmlElement(name = "concurrent")
  var concurrent: Int = 20000
  /**
   * 支持的关系查询集合
   */
  @XmlJavaTypeAdapter(classOf[ServiceMapAdapter])
  @XmlElement(name = "services")
  var services: util.Map[String, RpcService] = _
}
