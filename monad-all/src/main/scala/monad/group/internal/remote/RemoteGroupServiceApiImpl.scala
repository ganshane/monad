// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.group.internal.remote

import java.lang.reflect.Type
import java.util.{List => JList}

import com.google.gson.reflect.TypeToken
import com.google.gson.{Gson, JsonParser}
import monad.core.services.GroupServerApi
import monad.face.ApiConstants
import monad.face.config.GroupApiSupport
import monad.face.model.{GroupConfig, ResourceDefinition}
import monad.group.internal.MonadGroupExceptionCode
import monad.support.services.{HttpRestClient, MonadException, XmlLoader}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * 远程组服务API的实现类
 * @author jcai
 */
class RemoteGroupServiceApiImpl(groupApiSupport: GroupApiSupport, httpRestClient: HttpRestClient) extends GroupServerApi {
  private lazy val groupApi = groupApiSupport.groupApi
  private val logger = LoggerFactory getLogger getClass
  private val gson = new Gson

  def GetSelfGroupConfig: GroupConfig = {
    executeApiRequest[GroupConfig](ApiConstants.GROUP_GetSelfGroupConfig)
  }

  def GetOtherGroups: List[GroupConfig] = {
    val typeToken = new TypeToken[JList[GroupConfig]]() {}
    executeApiRequest[JList[GroupConfig]](ApiConstants.GROUP_GetOtherGroups, rawType = Some(typeToken.getType)).toList
  }

  private def executeApiRequest[T](api: String, params: Option[Map[String, String]] = None, rawType: Option[Type] = None)(implicit m: Manifest[T]): T = {
    var jsonStr: String = null
    try {
      jsonStr = httpRestClient.get(groupApi + "/" + api, params)
    } catch {
      case e: Throwable =>
        throw new MonadException("fail to connect group server " + e.toString, MonadGroupExceptionCode.FAIL_CONNECT_GROUP_SERVER)
    }
    val json = new JsonParser().parse(jsonStr).getAsJsonObject
    if (json.get(ApiConstants.SUCCESS).getAsBoolean) {
      var genericType: Type = null
      if (rawType.isDefined) {
        genericType = rawType.get
      } else {
        val clazz = m.erasure.asInstanceOf[Class[T]]
        genericType = TypeToken.get[T](clazz).getType
      }
      return gson.fromJson(json.get(ApiConstants.DATA), genericType).asInstanceOf[T]
    }
    throw new MonadException("fail to get self group config " + json.get(ApiConstants.MSG),
      MonadGroupExceptionCode.FAIL_GET_SELF_GROUP_CONFIG
    )
  }

  //get all resources
  def GetResources(group: Option[String]) = {
    val typeToken = new TypeToken[JList[String]]() {}

    executeApiRequest[JList[String]](ApiConstants.GROUP_GetResources, Some(Map("group" -> group.getOrElse(""))), rawType = Some(typeToken.getType)).map { content =>
      XmlLoader.parseXML[ResourceDefinition](content)
    }.toList
  }

  def GetCloudAddress = {
    executeApiRequest[String](ApiConstants.GROUP_GetCloudAddress)
  }
}
