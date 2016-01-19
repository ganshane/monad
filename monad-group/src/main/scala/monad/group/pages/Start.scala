// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.pages

import javax.servlet.http.HttpServletRequest

import monad.extjs.annotations.ExtDirectMethod
import org.apache.commons.io.IOUtils
import org.apache.tapestry5.annotations.Import
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.json.{JSONArray, JSONObject}
import org.apache.tapestry5.services.assets.AssetPathConstructor
import org.apache.tapestry5.services.javascript.JavaScriptSupport
import org.apache.tapestry5.util.TextStreamResponse

/**
 *
 * @author jcai
 */
@Import(
  stylesheet = Array[String]("classpath:assets/MonadGroup.css")
)
class Start {
  @Inject
  private var javaScriptSupport: JavaScriptSupport = _
  @Inject
  private var assetPathConstructor: AssetPathConstructor = _
  @Inject
  private var request: HttpServletRequest = _

  def getAppPath = assetPathConstructor.constructAssetPath("assets", "mg")

  def getAppJs = {
    assetPathConstructor.constructAssetPath("assets", "MonadGroup.js")
  }

  @ExtDirectMethod
  def show = {
    val requestJSON = new JSONObject(
      IOUtils.toString(request.getInputStream, "UTF-8")
    )
    val json = new JSONObject
    json.put("type", "rpc")
    json.put("action", requestJSON.getString("action"))
    json.put("method", requestJSON.getString("method"))
    json.put("tid", requestJSON.getString("tid"))
    val result = new JSONArray
    json.put("result", result)
    val obj1 = new JSONObject
    result.put(obj1)
    obj1.put("name", "wbswry")
    obj1.put("cn_name", "网吧上网人员")
    new TextStreamResponse("text/plain", json.toString)
  }
}
