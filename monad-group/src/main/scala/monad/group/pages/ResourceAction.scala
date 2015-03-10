// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.pages

import com.google.gson.JsonObject
import monad.extjs.annotations.ExtDirectMethod
import monad.extjs.model.ExtStreamResponse
import monad.face.model.ResourceDefinition
import monad.group.internal.{MonadGroupExceptionCode, MonadGroupManager}
import monad.support.services.{MonadException, XmlLoader}
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * resource action
 * @author jcai
 */
class ResourceAction {
  private val logger = LoggerFactory getLogger getClass
  @Inject
  private var monadGroupManager: MonadGroupManager = _

  @ExtDirectMethod
  def destroyRecord(params: JsonObject) = {
    logger.debug("params:{}", params)
    val resourceName = params.get("name").getAsString
    logger.info("delete resource:{}", resourceName)
    monadGroupManager.deleteResources(resourceName)
    new ExtStreamResponse
  }

  @ExtDirectMethod
  def resync(names: String) = {
    logger.info("resync resource:{}", names)
    names.split(",").foreach(monadGroupManager.resync)
    new ExtStreamResponse
  }

  @ExtDirectMethod
  def getResourceXml(name: String) =
    new ExtStreamResponse(monadGroupManager.getResource(name))

  @ExtDirectMethod
  def findExtensionValue(name: String) = {
    val r = monadGroupManager.getStat(name)
    val jsonObj = new JsonObject
    jsonObj.addProperty("quantity", String.valueOf(r._1))
    jsonObj.addProperty("maxValue", String.valueOf(r._2))
    new ExtStreamResponse(jsonObj)
  }


  @ExtDirectMethod
  def findAll = {
    new ExtStreamResponse(collection.JavaConversions.
      asJavaCollection(monadGroupManager.findResources))
  }

  @ExtDirectMethod
  def create(xml: String) = {
    logger.debug("xml:\n{}", xml)
    try {
      val rd = XmlLoader.parseXML[ResourceDefinition](xml,
        xsd = Some(getClass.getResourceAsStream("/resource.xsd")))
      if (InternalUtils.isBlank(rd.name)) {
        throw new MonadException("资源名称未定义",
          MonadGroupExceptionCode.MISSING_RESOURCE_NAME
        )
      }
      monadGroupManager.saveOrUpdateResource(rd, Some(xml))
    } catch {
      case NonFatal(e) =>
        throw MonadException.wrap(e)
    }
    new ExtStreamResponse
  }
}
