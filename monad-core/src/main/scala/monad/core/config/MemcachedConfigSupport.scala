// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * memcached configuration support
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MemcachedConfigSupport")
trait MemcachedConfigSupport {
  @XmlElement(name = "memcached")
  var memcached = new MemcacehdConfig
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MemcachedConfig")
class MemcacehdConfig {
  @XmlElement(name = "address")
  var servers: String = _
  //缓存过期时间
  @XmlElement(name = "expired_period_in_minutes")
  var expiredPeriodInMinutes: Int = 360
}
