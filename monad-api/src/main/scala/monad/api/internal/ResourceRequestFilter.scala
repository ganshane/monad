// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import monad.api.services.{MonadApiExceptionCode, ResourceRequest}
import monad.face.services.ResourceDefinitionLoader
import monad.support.services.MonadException
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.{Request, RequestFilter, RequestHandler, Response}

/**
 *
 * @author jcai
 */
class ResourceRequestFilter(resourceRequest: ResourceRequest,
                            loader: ResourceDefinitionLoader) extends RequestFilter {
  private val resourceParameter = "i"

  def service(request: Request, response: Response, handler: RequestHandler) = {
    val resourceName = request.getParameter(resourceParameter)
    if (InternalUtils.isNonBlank(resourceName)) {
      val rd = loader.getResourceDefinition(resourceName)
      rd match {
        case Some(r) =>
          resourceRequest.storeResourceDefinition(r)
        case None =>
          throw new MonadException("Resource %s not found".format(resourceName), MonadApiExceptionCode.RESOURCE_NOT_FOUND)
      }
    }
    handler.service(request, response)
  }
}
