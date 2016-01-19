// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api

import com.google.gson.JsonObject

/**
 * count record number
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.1
 */
class CountApi extends SearchApi {
  /**
   * @see monad.api.pages.api.SearchApi#doExecuteApi()
   */
  override def doExecuteApi(): JsonObject = {
    val json = query(includeData = false)
    //remove data,only get record number
    json.remove("data")
    json
  }
}
