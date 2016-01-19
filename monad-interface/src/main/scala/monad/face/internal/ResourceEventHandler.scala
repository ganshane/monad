// Copyright 2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.internal

import java.util.concurrent.atomic.AtomicBoolean

import com.lmax.disruptor.EventHandler
import monad.face.model.ResourceEvent
import monad.face.services.{GroupZookeeperTemplate, ResourceDefinitionLoaderListener}
import org.slf4j.LoggerFactory

/**
 * @author jcai
 */
class ResourceEventHandler(listener: ResourceDefinitionLoaderListener, zk: GroupZookeeperTemplate)
  extends EventHandler[ResourceEvent] {
  private val logger = LoggerFactory getLogger getClass
  private val threadNameFlag = new AtomicBoolean(false)

  def onEvent(event: ResourceEvent, sequence: Long, endOfBatch: Boolean) {
    if (threadNameFlag.compareAndSet(false, true)) {
      Thread.currentThread().setName("monad-background-ResourceEvent")
    }
    event.eventType match {
      case ResourceEvent.Start(version) =>
        listener.onResourceLoaded(event.resource, version)
      case ResourceEvent.Remove =>
        listener.onRemove(event.resource.name)
      case other =>
        logger.error("receive other message:{}", other)
    }
    event.reset()
  }
}
