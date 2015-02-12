// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.model

import monad.core.model.ResourceEvent.ResourceEventType
import monad.face.model.ResourceDefinition

/**
 * 资源事件类型
 * @author jcai
 */
class ResourceEvent {
  var resource: ResourceDefinition = _
  var eventType: ResourceEventType = null

  def reset() {
    resource = null; eventType = null
  }
}

object ResourceEvent {

  abstract class ResourceEventType

  case class Start(version: Int) extends ResourceEventType

  case object Remove extends ResourceEventType

}
