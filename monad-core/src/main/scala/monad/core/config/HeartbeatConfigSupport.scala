// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * Created by jcai on 14-8-22.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HeartbeatConfigSupport")
trait HeartbeatConfigSupport {
  @XmlElement(name = "heartbeat")
  var heartbeat = new HeartbeatConfig
}
