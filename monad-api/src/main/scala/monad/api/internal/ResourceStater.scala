// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import com.google.gson.JsonObject
import monad.api.services.SearcherFacade
import monad.face.model.ResourceDefinition
import monad.face.services.GroupZookeeperTemplate

/**
 * 针对资源的状态进行统计
 * @author jcai
 */
class ResourceStater(searcherFacade: SearcherFacade, groupZk: GroupZookeeperTemplate) {
  def stat(resource: ResourceDefinition): (Long, Long, Long) = {
    val indexNum = searcherFacade.getDocumentNum
    (indexNum, indexNum, 0)
  }

  def stat2(resource: ResourceDefinition): JsonObject = {
    groupZk.getRegionInfo(resource.name)
  }
}
