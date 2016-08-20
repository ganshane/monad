// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.base

import com.google.gson.{GsonBuilder, JsonObject}
import monad.api.MonadApiConstants
import monad.api.services.MonadApiExceptionCode
import monad.face.MonadFaceConstants
import org.apache.tapestry5.EventContext
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.Request
import org.apache.tapestry5.util.TextStreamResponse
import org.slf4j.Logger
import stark.utils.services.StarkException

import scala.util.control.NonFatal

/**
 * base api class
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.3
 */
abstract class BaseApi {
  @Inject
  private var logger: Logger = _
  @Inject
  private var request: Request = _

  //handle web request
  private def onActivate(context: EventContext): Any = {
    var json: JsonObject = null
    try {
      //execute api
      //val start = System.currentTimeMillis()
      json = doExecuteApi()
      //val end = System.currentTimeMillis()
      //logger.info("time:{}",end-start)
      if (json ne null) {
        json.addProperty(MonadApiConstants.STATUS, MonadApiConstants.OK_STATUS)
        json.addProperty(MonadApiConstants.JSON_KEY_SUCCESS, true)
      }
      else
        new StarkException("Fail to get result,result is null", MonadApiExceptionCode.FAIL_GET_RESULT)
    } catch {
      //catch any exception
      case NonFatal(e) =>
        json = new JsonObject
        json.addProperty(MonadApiConstants.JSON_KEY_SUCCESS, false)
        json.addProperty(MonadApiConstants.STATUS, MonadApiConstants.ERROR_STATUS)
        if (e.isInstanceOf[StarkException]) {
          json.addProperty(MonadApiConstants.MSG, e.getMessage)
        } else {
          logger.error(e.toString, e)
          json.addProperty(MonadApiConstants.MSG, e.toString)
        }
    }
    val callback = request.getParameter("callback")
    var jsonStr: String = null
    if (request.getParameter("pretty") != null) {
      val gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
      jsonStr = gson.toJson(json)
    } else
      jsonStr = MonadFaceConstants.GLOBAL_GSON.toJson(json)

    if (InternalUtils.isBlank(callback)) {
      //compact json string to output
      new TextStreamResponse(MonadApiConstants.JSON_MIME_TYPE, jsonStr)
    } else {
      new TextStreamResponse(MonadApiConstants.JSON_MIME_TYPE, callback + "(" + jsonStr + ");")
    }
  }

  /**
   * 执行对应的ＡＰＩ
   * @return api return result
   * @since 0.1
   */
  protected def doExecuteApi(): JsonObject
}
