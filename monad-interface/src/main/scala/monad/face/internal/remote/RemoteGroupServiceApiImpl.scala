// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.internal.remote

import java.io.StringReader
import java.lang.reflect.Type
import java.util.{List => JList}

import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.{Gson, JsonParser}
import monad.face.ApiConstants
import monad.face.config.GroupApiSupport
import monad.face.model.{GroupConfig, ResourceDefinition}
import monad.face.services.{GroupServerApi, MonadFaceExceptionCode}
import monad.support.services.{HttpRestClient, MonadException, XmlLoader}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

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

  private def executeApiRequest[T](api: String, params: Option[Map[String, String]] = None, rawType: Option[Type] = None)(implicit m: Manifest[T]): T = {
    var jsonStr: String = null
    try {
      jsonStr = httpRestClient.get(groupApi + "/" + api, params)
    } catch {
      case NonFatal(e) =>
        throw new MonadException("fail to connect group server " + e.toString, MonadFaceExceptionCode.FAIL_CONNECT_GROUP_SERVER)
    }
    val jsonReader = new JsonReader(new StringReader(jsonStr))
    jsonReader.setLenient(true)
    val json = new JsonParser().parse(jsonReader).getAsJsonObject
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
      MonadFaceExceptionCode.FAIL_GET_SELF_GROUP_CONFIG
    )
  }

  def GetOtherGroups: List[GroupConfig] = {
    val typeToken = new TypeToken[JList[GroupConfig]]() {}
    executeApiRequest[JList[GroupConfig]](ApiConstants.GROUP_GetOtherGroups, rawType = Some(typeToken.getType)).toList
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
