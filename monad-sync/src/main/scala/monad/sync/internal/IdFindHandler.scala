package monad.sync.internal

import com.lmax.disruptor.WorkHandler
import monad.face.MonadFaceConstants
import monad.face.services.ResourceDefinitionConversions.resourceDefinitionWrapper
import monad.protocol.internal.InternalIdProto.{AddIdRequest, AddIdResponse, IdCategory}
import monad.rpc.services.RpcClient
import monad.support.services.LoggerSupport
import monad.sync.model.DataEvent
import monad.sync.services.ResourceImporterManager

/**
 * find id from id server
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
class IdFindHandler(manager:ResourceImporterManager,rpcClient: RpcClient) extends WorkHandler[DataEvent] with LoggerSupport{

  override def onEvent(event: DataEvent): Unit = {

  //override def onEvent(event: DataEvent, sequence: Long, endOfBatch: Boolean): Unit = {
    val importer = manager.getObject(event.resourceName)
    importer.resourceDefinition.categoryProperty match{
      case Some((index,rp)) =>
        val objectIdValue = event.row(index).toString
        val category = IdCategory.valueOf(rp.objectCategory.toString)
        val addIdRequest = AddIdRequest.newBuilder().setCategory(category).setLabel(objectIdValue)
        var tryFlag = true
        while(tryFlag){
          val future = rpcClient.writeMessageWithBlocking(MonadFaceConstants.MACHINE_ID,AddIdRequest.cmd,addIdRequest.build)
          if(future.get != null){
            val commandResponse = future.get
            val response = commandResponse.getExtension(AddIdResponse.cmd)
            event.objectId = Some(response.getOrd)
            tryFlag = false
          }else{
            error("fail to get object id,id server down? ")
            Thread.sleep(2000)
          }
        }
      case None =>
        //do nothing
    }
  }
}
