// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.services

import com.google.gson.JsonObject
import com.lmax.disruptor.dsl.Disruptor
import monad.face.internal.AbstractResourceDefinitionLoaderListener
import monad.face.model.IndexEvent
import monad.face.services.RpcSearcherFacade
import monad.support.services.ServiceLifecycle
import org.apache.lucene.store.RateLimiter

/**
 * 资源索引管理器
 * @author jcai
 */
trait ResourceIndexerManager
  extends AbstractResourceDefinitionLoaderListener[ResourceIndexer]
  with RpcSearcherFacade
  with ServiceLifecycle {
  /**
   * 获得disruptor对象
   * @return Disruptor对象
   */
  def getDisruptor: Disruptor[IndexEvent]

  def getRateLimiter: Option[RateLimiter]

  def setRegionInfo(name: String, jsonObject: JsonObject);
}
