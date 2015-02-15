// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.api.internal

import monad.api.services.{ResourceRequest, SearcherQueue}
import monad.core.internal.AbstractResourceDefinitionLoaderListener
import monad.face.model.{ResourceDefinition, ResourceType}
import monad.face.services.RpcSearcherFacade
import monad.support.services.ZookeeperTemplate
import org.apache.tapestry5.ioc.services.Builtin

/**
 * 搜索的实现
 * @author jcai
 */
class SearcherQueueManagerImpl(resourceRequest: ResourceRequest, zookeeper: ZookeeperTemplate, @Builtin resourceSearcher: RpcSearcherFacade)
  extends AbstractResourceDefinitionLoaderListener[SearcherQueue] {
  protected def createObject(rd: ResourceDefinition, version: Int) = {
    if (ResourceType.Data == rd.resourceType) null
    else {
      val searcher = new SearcherQueueImpl(rd, resourceSearcher)
      searcher
    }
  }
}

