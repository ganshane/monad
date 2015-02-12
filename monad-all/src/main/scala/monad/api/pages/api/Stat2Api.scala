// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.pages.api

import com.google.gson.JsonObject
import monad.api.base.BaseApi
import monad.api.internal.ResourceStater
import monad.api.services.MonadApiExceptionCode
import monad.face.services.ResourceDefinitionLoader
import monad.support.services.MonadException
import org.apache.tapestry5.ioc.Messages
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request

/**
 * 查看某一资源的状态
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 3.0.4
 */
class Stat2Api extends BaseApi {
  @Inject
  private var request: Request = _
  @Inject
  private var resourceStater: ResourceStater = _
  @Inject
  private var messages: Messages = _
  @Inject
  private var loader: ResourceDefinitionLoader = _

  /**
   * @see monad.api.pages.api.SearchApi#doExecuteApi()
   */
  override def doExecuteApi(): JsonObject = {
    //index name
    val indexName = request.getParameter("i")
    if (InternalUtils.isBlank(indexName)) {
      throw new MonadException(messages.get("invalidate-parameter"), MonadApiExceptionCode.MISSING_RESOURCE_PARAMETER)
    }
    //get column definitions
    val definition = loader.getResourceDefinition(indexName)
    if (definition.isEmpty) {
      throw new MonadException(messages.get("fail-to-find-index"),
        MonadApiExceptionCode.RESOURCE_NOT_FOUND
      )
    }

    resourceStater.stat2(definition.get)
  }
}
