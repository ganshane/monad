// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import java.util.ArrayList
import monad.face.model.DynamicResourceDefinition.DynamicProperty
import javax.xml.bind.annotation._
import java.util

/**
 * 动态资源的特征定义
 * @author jcai
 */
object DynamicResourceDefinition{

  /**
   * 动态资源的特征属性
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class DynamicProperty{
    /**
     * 特征名称
     */
    @XmlAttribute(name="trait")
    var traitName:String = _
    /**
     * 特征列说明
     */
    @XmlAttribute(name="cn_name")
    var cnName:String = _
  }
}

/**
 * 动态资源的定义
 */
@XmlRootElement(name="dynamic")
@XmlAccessorType(XmlAccessType.FIELD)
class DynamicResourceDefinition {
  /**
   * 动态资源的属性
   */
  @XmlElementWrapper(name="properties")
  @XmlElement(name="property")
  var properties = new util.ArrayList[DynamicProperty]
}