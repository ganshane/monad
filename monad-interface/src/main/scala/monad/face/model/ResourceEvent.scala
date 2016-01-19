// Copyright 2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import monad.face.model.ResourceEvent.ResourceEventType

/**
 * 资源事件类型
 * @author jcai
 */
class ResourceEvent {
  var resource: ResourceDefinition = _
  var eventType: ResourceEventType = null

  def reset() {
    resource = null;
    eventType = null
  }
}

object ResourceEvent {

  abstract class ResourceEventType

  case class Start(version: Int) extends ResourceEventType

  case object Remove extends ResourceEventType

}
