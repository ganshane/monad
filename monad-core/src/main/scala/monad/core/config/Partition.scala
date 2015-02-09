// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlAttribute, XmlType}

/**
 * Created by jcai on 14-8-21.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Partition")
class Partition {
  @XmlAttribute(name = "id")
  var id: Short = _
  @XmlAttribute(name = "weight")
  var weight: Int = 1

  override def equals(obj: scala.Any): Boolean = {
    if (!obj.isInstanceOf[Partition])
      return false
    val otherId = obj.asInstanceOf[Partition].id
    otherId == id
  }
}
