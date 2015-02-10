// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * Created by jcai on 14-8-21.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LogFileSupport")
trait LogFileSupport {
  @XmlElement(name = "log_file")
  var logFile: String = _
}
