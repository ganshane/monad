// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import monad.face.model.ResourceRelation.Rel
import javax.xml.bind.annotation._
import java.util.ArrayList
import java.util


/**
 * 资源关系分析的定义
 * @author jcai
 */
object ResourceRelation{


  /**
   * 关联特征属性
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class RelationProperty{
    /**
     * 关联的名称
     */
    @XmlAttribute
    var name:String=_
    /**
     * 特征属性
     */
    @XmlAttribute(name="trait")
    var traitProperty:String=_
    /**
     * 定义值的处理方法
     */
    @XmlAttribute(name="value_process")
    var valueProcess:String=_
    /**
     * 是否支持自定义查询
     * @since 4.0.5
     */
    @XmlAttribute(name="custom_query_support")
    var custom:Boolean=_
  }

  /**
   * 关联定义
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class Rel{
    /** 特征名称 **/
    @XmlAttribute
    var name:String = _
    /** 中文名 **/
    @XmlAttribute(name="cn_name")
    var cnName:String = _
    /** 对应资源名 **/
    @XmlAttribute
    var resource:String = _
    @XmlElement(name="desc")
    var desc:String = _
    /** 特征列的定义集合 **/
    @XmlElementWrapper(name="properties")
    @XmlElement(name="property")
    var properties = new util.ArrayList[RelationProperty]()
  }
}

/**
 * 资源关系
 */
@XmlRootElement(name="relations")
@XmlAccessorType(XmlAccessType.FIELD)
class ResourceRelation {
  /**
   * 资源关系集合定义
   */
  @XmlElement(name="rel")
  var relations = new util.ArrayList[Rel]
}