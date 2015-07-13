package monad.id.internal

import monad.face.MonadFaceConstants
import monad.id.services.IdService
import monad.protocol.internal.InternalIdProto.{AddIdRequest, AddIdResponse, GetIdLabelRequest, GetIdLabelResponse}
import monad.rpc.config.RpcBindSupport
import monad.rpc.model.RpcServerLocation
import monad.rpc.protocol.CommandProto.BaseCommand
import monad.rpc.services.{CommandResponse, RpcServerListener, RpcServerMessageFilter, RpcServerMessageHandler}
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
        val ord = getIdRequest.getOrd
        val labelOpt = idService.getIdLabel(category,ord)
        labelOpt match {
          case Some(label) =>
            val getIdResponse = GetIdLabelResponse.newBuilder().setLabel(label).build()
            response.writeMessage(commandRequest,GetIdLabelResponse.cmd,getIdResponse)
          case None =>
            response.writeErrorMessage(commandRequest,"label not found")
        }
        true
      }else{
        handler.handle(commandRequest,response)
      }
    }
  }
}
class IdRpcServerListener(zk: ZookeeperTemplate, rpc: RpcBindSupport) extends RpcServerListener {
  override def afterStop(): Unit = ()

  override def afterStart(): Unit = {
    val rpcServerLocation = RpcServerLocation.exposeRpcLocation(rpc.rpc)
    zk.createEphemeralPathWithStringData(
      MonadFaceConstants.MACHINE_ID,
      Some(rpcServerLocation.toJSON.toCompactString))
  }
}
