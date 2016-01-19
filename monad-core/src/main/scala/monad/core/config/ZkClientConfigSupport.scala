// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * zookeeper 相关配置
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZkConfigSupport")
trait ZkClientConfigSupport {
  @XmlElement(name = "zk")
  var zk = new ZkConfig
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZkConfig")
class ZkConfig {
  @XmlElement(name = "address")
  var address: String = _

  @XmlElement(name = "root")
  var root: String = "/monad"

  @XmlElement(name = "timeout_in_mills")
  var timeoutInMills: Int = 60000
}
