// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * database configuration support
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-01-17
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatabaseConfigSupport")
trait DatabaseConfigSupport {
  @XmlElement(name = "database")
  var db: DatabaseConfig = new DatabaseConfig()
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatabaseConfig")
class DatabaseConfig {
  @XmlElement(name = "pool_name")
  var poolName: String = "monad"
  @XmlElement(name = "driver")
  var driver: String = _
  @XmlElement(name = "user")
  var user: String = _
  @XmlElement(name = "password")
  var password: String = _
  @XmlElement(name = "url")
  var url: String = _
}
