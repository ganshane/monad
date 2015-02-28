// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * Created by jcai on 14-8-22.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HeartbeatConfigSupport")
class HeartbeatConfig {
  @XmlElement(name = "host_name")
  var hostName: String = _
  @XmlElement(name = "cron")
  var cron: String = "0/5 * * * * ? *"
}
