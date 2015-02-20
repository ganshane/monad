// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.protocol.internal.CommandProto.BaseCommand
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.rpc.services.{CommandResponse, RpcServerMessageFilter, RpcServerMessageHandler}
import monad.sync.services.ResourceImporterManager

/**
 * for processor sync request
 */
object SyncMessageFilter {

  /**
   * 内部数据同步消息处理，通常是spout和processor来请求数据
   * @param syncNoSQLService sync nosql service
   */
  class InternalDataSyncRequestHandlerFilter(resourceImporterManager: ResourceImporterManager)
    extends RpcServerMessageFilter {
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if (!commandRequest.hasExtension(SyncRequest.cmd))
        return handler.handle(commandRequest, response)

      val syncRequest = commandRequest.getExtension(SyncRequest.cmd)
      val syncResponse = resourceImporterManager.fetchSyncData(syncRequest)
      response.writeMessage(SyncResponse.cmd, syncResponse)

      true
    }
  }

}
