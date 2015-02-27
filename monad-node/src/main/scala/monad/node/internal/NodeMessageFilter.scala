package monad.node.internal

import monad.face.services.RpcSearcherFacade
import monad.protocol.internal.CommandProto.BaseCommand
import monad.protocol.internal.InternalMaxdocQueryProto.{MaxdocQueryRequest, MaxdocQueryResponse}
import monad.rpc.services.{CommandResponse, RpcServerMessageFilter, RpcServerMessageHandler}

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
      maxDocResponse.setMaxdoc(maxDoc)
      maxDocResponse.setResourceName(request.getResourceName)

      response.writeMessage(commandRequest, MaxdocQueryResponse.cmd, maxDocResponse.build())

      true
    }
  }

}
