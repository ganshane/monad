// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import javax.xml.bind.annotation.{XmlAttribute, XmlElement, XmlType}

/**
 * 针对某一分组的定义
 * @author jcai
 */
@XmlType(name = "GroupConfig")
class GroupConfig {
  /** 组的唯一标示 **/
  @XmlAttribute
  var id: String = _
  /** 组的中文名称 **/
  @XmlElement(name = "cn_name")
  var cnName: String = _
  /** 提供api服务的URL **/
  @XmlElement(name = "api_url")
  var apiUrl: String = _
}
