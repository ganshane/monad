// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai. 
 * site: http://www.ganshane.com
 */

package monad.api.pages.api


import com.google.gson.JsonObject
import monad.api.MonadApiConstants
import monad.api.base.BaseApi
import monad.api.services.{MonadApiExceptionCode, SearcherFacade}
import monad.support.services.MonadException
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request


/**
 * monitor api
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.1
 */
class MonitorApi extends BaseApi {
  @Inject
  var searchFacade: SearcherFacade = _
  @Inject
  var request: Request = _

  /**
   *
   * @see monad.core.node.api.base.BaseApi#doExecuteApi()
   */
  override def doExecuteApi(): JsonObject = {
    val indexName = request.getParameter("i")
    val json = new JsonObject
    if (!InternalUtils.isBlank(indexName)) {
      json.addProperty(MonadApiConstants.JSON_KEY_TOTAL_RECORD_NUM, searchFacade.getDocumentNum)
      return json
    }
    throw new MonadException("请加入 i (index name)参数", MonadApiExceptionCode.MISSING_RESOURCE_PARAMETER)
  }
}
