// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * Created by jcai on 14-8-21.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LocalStoreSupport")
trait LocalStoreConfigSupport {
  @XmlElement(name = "local_store_dir")
  var localStoreDir: String = _
}
