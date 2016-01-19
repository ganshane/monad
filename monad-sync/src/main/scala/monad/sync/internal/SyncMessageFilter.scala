// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.rpc.protocol.CommandProto
import monad.rpc.protocol.CommandProto.BaseCommand
import monad.rpc.services.{CommandResponse, RpcServerMessageFilter, RpcServerMessageHandler}
import monad.sync.services.ResourceImporterManager

/**
 * for processor sync request
 */
object SyncMessageFilter {

  /**
   * 内部数据同步消息处理，通常是spout和processor来请求数据
   */
  class InternalDataSyncRequestHandlerFilter(resourceImporterManager: ResourceImporterManager)
    extends RpcServerMessageFilter {
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if (!commandRequest.hasExtension(SyncRequest.cmd))
        return handler.handle(commandRequest, response)

      val syncRequest = commandRequest.getExtension(SyncRequest.cmd)
      val syncResponse = resourceImporterManager.fetchSyncData(syncRequest)
      response.writeMessage(commandRequest, SyncResponse.cmd, syncResponse)

      true
    }
  }

}
