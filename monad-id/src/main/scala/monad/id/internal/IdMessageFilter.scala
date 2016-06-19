// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id.internal

import monad.core.MonadCoreConstants
import monad.face.MonadFaceConstants
import monad.id.config.MonadIdConfig
import monad.id.services.IdService
import monad.protocol.internal.InternalIdProto._
import stark.rpc.model.RpcServerLocation
import stark.rpc.protocol.CommandProto.BaseCommand
import stark.rpc.services.{CommandResponse, RpcServerListener, RpcServerMessageFilter, RpcServerMessageHandler}
import monad.support.services.ZookeeperTemplate

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
object IdMessageFilter {
  class InternalAddIdRequestFilter(idService:IdService) extends RpcServerMessageFilter{
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if(commandRequest.hasExtension(AddIdRequest.cmd)){
        val addIdRequest = commandRequest.getExtension(AddIdRequest.cmd)
        val category = addIdRequest.getCategory
        val label = addIdRequest.getLabel
        val ordOpt = idService.getOrAddId(category,label)
        ordOpt match{
          case Some(ord) =>
            val addIdResponse = AddIdResponse.newBuilder().setOrd(ord).build()
            response.writeMessage(commandRequest,AddIdResponse.cmd,addIdResponse)

          case None =>
            response.writeErrorMessage(commandRequest,"fail to add category")
        }

        true
      }else{
        handler.handle(commandRequest,response)
      }
    }
  }
  class InternalGetIdLabelRequestFilter(idService:IdService) extends RpcServerMessageFilter{
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if(commandRequest.hasExtension(GetIdLabelRequest.cmd)){
        val getIdRequest = commandRequest.getExtension(GetIdLabelRequest.cmd)
        val category = getIdRequest.getCategory
        val ordIt= getIdRequest.getOrdList.iterator()
        val responseBuilder = GetIdLabelResponse.newBuilder()
        while(ordIt.hasNext){
          val ord = ordIt.next()
          val labelOpt = idService.getIdLabel(category,ord)
          responseBuilder.addLabel(labelOpt.getOrElse(""))
        }
        response.writeMessage(commandRequest,GetIdLabelResponse.cmd,responseBuilder.build())
        true
      }else{
        handler.handle(commandRequest,response)
      }
    }
  }
  class InternalBatchAddIdRequestFilter(idService:IdService) extends RpcServerMessageFilter{
    override def handle(commandRequest: BaseCommand, response: CommandResponse, handler: RpcServerMessageHandler): Boolean = {
      if(commandRequest.hasExtension(BatchAddIdRequest.cmd)){
        val addIdRequest = commandRequest.getExtension(BatchAddIdRequest.cmd)
        val category = addIdRequest.getCategory
        val it = addIdRequest.getLabelList.iterator()
        val responseBuilder = BatchAddIdResponse.newBuilder()
        while(it.hasNext){
          val ordOpt = idService.getOrAddId(category,it.next())
          responseBuilder.addOrd(ordOpt.getOrElse(MonadFaceConstants.UNKNOWN_ID_SEQ))
        }
        response.writeMessage(commandRequest,BatchAddIdResponse.cmd,responseBuilder.build())
        true
      }else{
        handler.handle(commandRequest,response)
      }
    }
  }
}
class IdRpcServerListener(zk: ZookeeperTemplate, config: MonadIdConfig) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.exposeRpcLocation(config.rpc)
    if(config.id.groups != null){
      config.id.groups.split(",").foreach{g=>
        zk.createEphemeralPathWithStringData(
          MonadCoreConstants.GROUPS_PATH+"/"+g+
            MonadFaceConstants.MACHINE_ID,
          Some(rpcServerLocation.toJSON.toCompactString))
      }
    }
  }
}
