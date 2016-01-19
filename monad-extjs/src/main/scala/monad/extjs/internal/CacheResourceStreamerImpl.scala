// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import monad.extjs.MonadExtjsConstants._
import org.apache.tapestry5.internal.services.ResourceStreamer
import org.apache.tapestry5.ioc.Resource
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.assets.StreamableResource
import org.apache.tapestry5.services.{Request, Response}
/**
 * 实现了对内容的缓存，尤其是静态资源
 * @author jcai
 */
class CacheResourceStreamerImpl(productionMode: Boolean,
                                request: Request,
                                response: Response,
                                delegate: ResourceStreamer)
  extends ResourceStreamer {

    def streamResource(resource: Resource) {
        if (doCacheAction) return
        delegate.streamResource(resource)
    }

  private def doCacheAction: Boolean = {
    if (productionMode) {
      response.setHeader(CACHE_CONTROL, CACHE_CONTROL_VALUE)
      if (!InternalUtils.isBlank(request.getHeader(CACHE_HEADER))) {
        response.setStatus(304)
        return true
      }
    }
    false
    }

    def streamResource(resource: StreamableResource) {
        if (doCacheAction) return
        delegate.streamResource(resource)
    }
}
