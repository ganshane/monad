// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.face.model

import java.util
import javax.xml.bind.annotation.{XmlAttribute, XmlElement, XmlRootElement}

import monad.face.model.ApiLocation.API

/**
 * API的位置
 * @author jcai
 */
object ApiLocation {

  class API {
    /**
     * 名称
     */
    @XmlAttribute
    var name: String = _
    /**
     * API对应的URL地址
     */
    @XmlAttribute
    var url: String = _
    /**
     * 对应的publicKey
     */
    @XmlAttribute(name = "public_key")
    var publicKey: String = _
  }

}

@XmlRootElement(name = "locations")
class ApiLocation {
  /**
   * API位置列表
   */
  @XmlElement(name = "api")
  var apis = new util.ArrayList[API]
}
