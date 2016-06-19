// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import com.google.protobuf.ByteString
import monad.face.services.{BitSetUtils, RpcSearcherFacade}
import monad.protocol.internal.InternalFindDocProto.{InternalFindDocRequest, InternalFindDocResponse}
import monad.protocol.internal.InternalIdProto.{IdSearchRequest, IdSearchResponse}
import monad.protocol.internal.InternalMaxdocQueryProto.{MaxdocQueryRequest, MaxdocQueryResponse}
import monad.protocol.internal.InternalSearchProto.{InternalSearchRequest, InternalSearchResponse}
import stark.rpc.protocol.CommandProto.BaseCommand
import stark.rpc.services.{CommandResponse, RpcServerMessageFilter, RpcServerMessageHandler}

/**
 * node message filter
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-27
 */
object NodeMessageFilter {

  class MaxdocMessageFilter(searcher: RpcSearcherFacade) extends RpcServerMessageFilter {
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if (!commandRequest.hasExtension(MaxdocQueryRequest.cmd)) {
        return handler.handle(commandRequest, response)
      }

      val request = commandRequest.getExtension(MaxdocQueryRequest.cmd)
      val maxDoc = searcher.maxDoc(request.getResourceName)

      val maxDocResponse = MaxdocQueryResponse.newBuilder()
      maxDocResponse.setMaxdoc(maxDoc.toInt)
      maxDocResponse.setResourceName(request.getResourceName)

      response.writeMessage(commandRequest, MaxdocQueryResponse.cmd, maxDocResponse.build())

      true
    }
  }

  class InternalSearchMessageFilter(searcher: RpcSearcherFacade) extends RpcServerMessageFilter {
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if (!commandRequest.hasExtension(InternalSearchRequest.cmd)) {
        return handler.handle(commandRequest, response)
      }

      val request = commandRequest.getExtension(InternalSearchRequest.cmd)
      val shardResult = searcher.collectSearch(request.getResourceName, request.getQ, request.getSort, request.getTopN)
      val searchResponse = InternalSearchResponse.newBuilder()
      searchResponse.setResourceName(request.getResourceName)
      searchResponse.setMaxdoc(shardResult.maxDoc)
      searchResponse.setMaxScore(shardResult.maxScore)
      searchResponse.setTotal(shardResult.totalRecord)
      searchResponse.setPartitionId(shardResult.serverHash)

      shardResult.results.foreach { s =>
        val r = searchResponse.addResultsBuilder()
        r.setId(s._1)
        r.setScore(s._2)
      }

      response.writeMessage(commandRequest, InternalSearchResponse.cmd, searchResponse.build())

      true
    }
  }

  class InternalFindDocRequestMessageFilter(searcher: RpcSearcherFacade) extends RpcServerMessageFilter {
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if (!commandRequest.hasExtension(InternalFindDocRequest.cmd)) {
        return handler.handle(commandRequest, response)
      }

      val request = commandRequest.getExtension(InternalFindDocRequest.cmd)
      val result = searcher.findObject(12, request.getResourceName, request.getId)
      val findDocResponse = InternalFindDocResponse.newBuilder()
      findDocResponse.setResourceName(request.getResourceName)
      result match {
        case Some(bytes) =>
          findDocResponse.setJson(ByteString.copyFrom(bytes))
        case None =>
        //do nothing
      }

      response.writeMessage(commandRequest, InternalFindDocResponse.cmd, findDocResponse.build())

      true
    }
  }

  class InternalIdSearchRequestMessageFilter(searcher:RpcSearcherFacade) extends RpcServerMessageFilter{
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if (!commandRequest.hasExtension(IdSearchRequest.cmd)) {
        return handler.handle(commandRequest, response)
      }

      val request = commandRequest.getExtension(IdSearchRequest.cmd)
      val result = searcher.searchObjectId(request.getResourceName,request.getQ)
      val idSearchResponse = IdSearchResponse.newBuilder()
      idSearchResponse.setPartitionId(result.region)
      val bs = ByteString.copyFrom(BitSetUtils.serialize(result.data))
      idSearchResponse.setBitset(bs)
      response.writeMessage(commandRequest,IdSearchResponse.cmd,idSearchResponse.build())

      true
    }
  }

}
