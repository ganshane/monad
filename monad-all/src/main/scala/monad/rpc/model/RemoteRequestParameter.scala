// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.rpc.model

import monad.face.annotation.Rpc
import monad.face.model.ResourceDefinition

import scala.collection.mutable

/**
 * 远程请求时候的参数
 * @author jcai
 */
class RemoteRequestParameter(val interfaceName: String,
                             val methodId: Int,
                             val rpcConfig: Rpc) {
  val parameters = mutable.Buffer[Any]()
  var regionOpt: Option[Short] = None
  var resourceDefinition: ResourceDefinition = null
  var tid: Int = -1

  def put(p: Any) {
    parameters.append(p)
  }

  def put(p: Int) {
    parameters.append(p)
  }

  def put(p: Long) {
    parameters.append(p)
  }

  def put(p: Float) {
    parameters.append(p)
  }

  def put(p: Short) {
    parameters.append(p)
  }

  def put(p: Double) {
    parameters.append(p)
  }

  def setRegion(region: Short) {
    regionOpt = Some(region)
  }

  override def equals(p1: Any): Boolean = {
    if (p1 != null && p1.isInstanceOf[RemoteRequestParameter]) {
      val p = p1.asInstanceOf[RemoteRequestParameter]
      return p.interfaceName == interfaceName && p.methodId == methodId && p.parameters == parameters
    }
    false
  }
}

