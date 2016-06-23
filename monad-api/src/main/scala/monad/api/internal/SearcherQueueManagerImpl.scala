// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.api.internal

import monad.api.services.{ResourceRequest, SearcherQueue}
import monad.face.internal.AbstractResourceDefinitionLoaderListener
import monad.face.model.{ResourceDefinition, ResourceType}
import monad.face.services.RpcSearcherFacade
import stark.utils.services.ZookeeperTemplate

/**
 * 搜索的实现
 * @author jcai
 */
class SearcherQueueManagerImpl(resourceRequest: ResourceRequest, zookeeper: ZookeeperTemplate, resourceSearcher: RpcSearcherFacade)
  extends AbstractResourceDefinitionLoaderListener[SearcherQueue] {
  protected def createObject(rd: ResourceDefinition, version: Int) = {
    if (ResourceType.Data == rd.resourceType) null
    else {
      val searcher = new SearcherQueueImpl(rd, resourceSearcher)
      searcher
    }
  }
}

