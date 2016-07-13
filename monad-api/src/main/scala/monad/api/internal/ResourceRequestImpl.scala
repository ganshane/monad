// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import monad.api.services.ResourceRequest
import roar.api.meta.ResourceDefinition

/**
 * 实现resource request
 *
 * @author jcai
 */
class ResourceRequestImpl extends ResourceRequest {
  private var rd: ResourceDefinition = null

  def getResourceDefinition = this.rd

  def storeResourceDefinition(rd: ResourceDefinition) {
    this.rd = rd
  }
}
