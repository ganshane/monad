// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.api.internal

import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date

import com.google.gson.{JsonArray, JsonObject, JsonParser}
import monad.api.MonadApiConstants
import monad.api.model.{SearchRequest, SearchResult}
import monad.api.services.{DBObjectExtractor, MemcachedClient, ObjectIdCreator}
import monad.face.MonadFaceConstants
import monad.face.config.ApiConfigSupport
import monad.face.model.ResourceDefinition
import monad.face.services.ResourceDefinitionConversions._
import monad.face.services.{DataTypeUtils, RpcSearcherFacade}
import stark.utils.StarkUtilsConstants
import stark.utils.services.SymbolExpander
import org.apache.commons.codec.digest.DigestUtils
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * 针对搜索结果的二次处理
 * @author jcai
 */
object SearchResultExtractor {
  private val DEFAULT_CREATE_FORMATTER = "yyyy-MM-dd HH:mm:ss"

  object DefaultDBObjectExtractor extends DBObjectExtractor {
    def extract(resource: ResourceDefinition, dbObj: JsonObject, highlighterObj: Option[ResultHighlighter] = None) = {
      val row = new JsonObject()
      row.addProperty(MonadFaceConstants.OBJECT_ID_FIELD_NAME, dbObj.get(MonadFaceConstants.OBJECT_ID_FIELD_NAME).getAsString)
      resource.properties.foreach { col =>
        val v = col.readApiValue(dbObj)
        if (v.isDefined) {
          var apiValue = v.get
          if (highlighterObj.isDefined) {
            apiValue = highlighterObj.get.highlight(col.name, apiValue, 3)
          }
          row.addProperty(col.name, apiValue)
        }
      }
      //如果有创建时间，需要格式化输出
      val createTimeFlag = dbObj.has(MonadFaceConstants.UPDATE_TIME_FIELD_NAME)
      if (createTimeFlag) {
        val formatter = new SimpleDateFormat(DEFAULT_CREATE_FORMATTER)
        row.addProperty(
          MonadFaceConstants.UPDATE_TIME_FIELD_NAME,
          formatter.format(new Date(DataTypeUtils.convertIntAsDate(dbObj.get(MonadFaceConstants.UPDATE_TIME_FIELD_NAME).getAsInt))))
      }

      Some(row)
    }
  }

  object DynamicDBObjectExtractor extends DBObjectExtractor {
    def extract(resource: ResourceDefinition, dbObj: JsonObject, highlighterObj: Option[ResultHighlighter] = None) = {
      val row = new JsonObject()
      row.addProperty(MonadFaceConstants.OBJECT_ID_FIELD_NAME,
        dbObj.get(MonadFaceConstants.OBJECT_ID_FIELD_NAME).getAsString)

      val dpMap = resource.dynamicType.properties.foldLeft(Map[String, String]()) { (map, x) =>
        map + (x.name -> x.traitProperty)
      }
      var valueMap = Map.newBuilder[String, String]
      resource.properties.foreach { col =>
        val v = col.readApiValue(dbObj)
        valueMap += (col.name -> v.getOrElse(""))
        if (v.isDefined) {
          var apiValue = v.get

          if (highlighterObj.isDefined) {
            apiValue = highlighterObj.get.highlight(col.name, apiValue, 3)
          }
          val traitPro = dpMap.get(col.name)
          val proKey = traitPro.getOrElse(col.name)
          row.addProperty(proKey, apiValue)
        }
      }
      val descFormat = resource.dynamicType.descFormat
      if (!InternalUtils.isBlank(descFormat)) {
        val desc = SymbolExpander.expand(descFormat.replaceAll("\\{", "\\${"), valueMap.result())
        row.addProperty(MonadFaceConstants.DYNAMIC_DESC, desc)
      }
      valueMap.clear()
      valueMap = null

      Some(row)
    }
  }

}

class SearchResultExtractor(noSQL: RpcSearcherFacade,
                            cacheSupport: ApiConfigSupport,
                            memcachedClient: MemcachedClient,
                            objectIdCreator: ObjectIdCreator) {
  private final val jsonParser = new JsonParser
  private val logger = LoggerFactory getLogger getClass

  def extract(searchRequest: SearchRequest,
              searchFunction: (SearchRequest) => SearchResult,
              highlighterObj: Option[ResultHighlighter] = None): JsonObject = {
    logger.info("[{}] searching [{}] ....", searchRequest.resourceName, searchRequest.q)
    val startTime = new Date().getTime

    val f2 = () => {
      directQuery(searchRequest, searchFunction, highlighterObj)
    }
    //if enable,show search results
    val result = if (cacheSupport.api.enableMemcachedCache) {
      //produce cache key
      val key: String = produceCacheKey(searchRequest)
      memcachedClient.getOrElse(key, f2)
    } else f2()

    val duration = new Date().getTime - startTime
    var total = 0
    if (result.has(MonadApiConstants.JSON_KEY_TOTAL))
      total = result.get(MonadApiConstants.JSON_KEY_TOTAL).getAsInt
    logger.info("[" + searchRequest.resourceName + "] [" + searchRequest.q + "],time:{},hits:{}", duration, total)

    result.addProperty("time", duration)

    result
  }

  private def directQuery(searchRequest: SearchRequest,
                          searchFunction: (SearchRequest) => SearchResult,
                          highlighterObj: Option[ResultHighlighter] = None): JsonObject = {
    val json = new JsonObject
    //查询某一具体数据
    if (searchRequest.objectId != null) {
      val buffer = ByteBuffer.wrap(searchRequest.objectId)
      val serverHash: Short = buffer.getShort
      //DataTypeUtils.convertAsShort(searchRequest.objectId)
      //System.arraycopy(searchRequest.objectId, 2, objId, 0, 4)
      val objId = buffer.getInt

      val row = findData(searchRequest, serverHash, objId, highlighterObj)
      row match {
        case Some(x) =>
          json.addProperty(MonadApiConstants.JSON_KEY_TOTAL, 1)
          json.add(MonadApiConstants.JSON_KEY_DATA, x)
        case None =>
          json.addProperty(MonadApiConstants.JSON_KEY_TOTAL, 0)
      }
      return json
    }

    //执行全文检索
    val result = searchFunction(searchRequest)

    json.addProperty(MonadApiConstants.JSON_KEY_ALL, result.all)
    json.addProperty(MonadApiConstants.JSON_KEY_TOTAL, result.hitCount)
    json.addProperty(MonadApiConstants.JSON_KEY_NODE_ALL, result.nodeAll)
    json.addProperty(MonadApiConstants.JSON_KEY_NODE_SUCCESS, result.nodeSuccess)
    json.add(MonadApiConstants.JSON_KEY_NODE_SUCCESS_INFO, result.nodeSuccessInfo)
    json.addProperty(MonadApiConstants.JSON_KEY_NODE_ERROR, result.nodeError)

    //convert to JsonObject
    if (searchRequest.includeData || cacheSupport.api.enableMemcachedCache) {
      appendData(result, json, searchRequest, highlighterObj)
    }
    json
  }

  private[api] def appendData(result: SearchResult, json: JsonObject, searchRequest: SearchRequest, highlighterObj: Option[ResultHighlighter] = None) = {
    val data = new JsonArray()
    val faceCountData = result.facetCount
    val servers = result.servers
    for (i <- 0 until result.hits.length) {
      val x = result.hits(i)
      val row = findData(searchRequest, servers(i), x, highlighterObj)
      if (row.isDefined) {
        val rowData = row.get
        if (faceCountData.isDefined) {
          rowData.addProperty(MonadFaceConstants.FACET_COUNT, faceCountData.get(i))
        }
        data.add(rowData)
      }
    }
    json.add(MonadApiConstants.JSON_KEY_DATA, data)
  }

  //从dfs中抓取数据
  private def findData(searchRequest: SearchRequest, serverHash: Short, x: Int, highlighterObj: Option[ResultHighlighter] = None): Option[JsonObject] = {
    val dbObj = noSQL.findObject(serverHash, searchRequest.resourceName, x)
    if (dbObj.isEmpty) {
      logger.warn("[{}]fetch from nosql is null with key:{}", searchRequest.resource.name, x)
    } else {
      val json = jsonParser.parse(new String(dbObj.get, StarkUtilsConstants.UTF8_ENCODING)).getAsJsonObject
      val bytes = ByteBuffer.allocate(6).putShort(serverHash).putInt(x).array()
      json.addProperty(MonadFaceConstants.OBJECT_ID_FIELD_NAME, objectIdCreator.objectIdToString(bytes))
      val extractor = searchRequest.dbObjectExtractor.getOrElse(SearchResultExtractor.DefaultDBObjectExtractor)
      return extractor.extract(searchRequest.resource, json, highlighterObj)
    }
    None
  }

  /**
   * produce cache key
   */
  private def produceCacheKey(searchRequest: SearchRequest): String = {
    //add memcached
    val keyBuffer =
      new StringBuilder()
    keyBuffer.append(searchRequest.resourceName).append("#")
      .append(searchRequest.q).append("#")
      .append(searchRequest.start).append("#")
      .append(searchRequest.offset).append("#")
      .append(searchRequest.sort)

    if (searchRequest.objectId != null) {
      keyBuffer.append("#").append(searchRequest.objectId)
    }
    DigestUtils.md5Hex(keyBuffer.toString())
  }
}
