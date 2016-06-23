// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import com.lmax.disruptor.WorkHandler
import monad.face.MonadFaceConstants
import monad.face.services.IdFacade
import monad.face.services.ResourceDefinitionConversions.resourceDefinitionWrapper
import stark.utils.services.LoggerSupport
import monad.sync.model.DataEvent
import monad.sync.services.ResourceImporterManager

/**
 * find id from id server
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
class IdFindHandler(manager:ResourceImporterManager,idFacade: IdFacade) extends WorkHandler[DataEvent] with LoggerSupport{

  override def onEvent(event: DataEvent): Unit = {

  //override def onEvent(event: DataEvent, sequence: Long, endOfBatch: Boolean): Unit = {
    val importer = manager.getObject(event.resourceName)
    importer.resourceDefinition.categoryProperty match{
      case Some((index,rp)) =>
        val objectIdValue = event.row(index).toString
        val category = rp.objectCategory.toString
        var tryFlag = true
        while(tryFlag){
          val id = idFacade.putIfAbsent(category,objectIdValue)
          if(id != MonadFaceConstants.UNKNOWN_ID_SEQ){
            event.objectId = Some(id)
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
