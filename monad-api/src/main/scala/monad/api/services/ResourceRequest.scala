// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.services

import monad.face.model.ResourceDefinition
import org.apache.tapestry5.ioc.ScopeConstants
import org.apache.tapestry5.ioc.annotations.Scope

/**
 * 资源请求的线程绑定类，方便进行处理
 * @author jcai
 */
@Scope(ScopeConstants.PERTHREAD)
trait ResourceRequest {
  def getResourceDefinition: ResourceDefinition

  def storeResourceDefinition(rd: ResourceDefinition)
}
