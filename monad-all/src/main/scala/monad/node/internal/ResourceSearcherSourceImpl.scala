// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.ExecutorService

import monad.face.MonadFaceConstants
import monad.face.model.ResourceDefinition
import monad.face.services.{ResourceSearcherFactory, ResourceSearcherSource}
import org.apache.lucene.index.IndexWriter

/**
 *
 * @author jcai
 */
class ResourceSearcherSourceImpl(factories: java.util.Map[String, ResourceSearcherFactory]) extends ResourceSearcherSource {
  def newResourceSearcher(rd: ResourceDefinition, indexWriter: IndexWriter, regionId: Short, executor: ExecutorService) = {
    var obj = factories.get(rd.name)
    if (obj == null)
      obj = factories.get(MonadFaceConstants.DEFAULT_RESOURCE_SEARCHER_FACTORY)
    obj.createSearcher(rd, indexWriter, regionId, executor)
  }
}

class DefaultResourceSearcherFactory extends ResourceSearcherFactory {
  def createSearcher(rd: ResourceDefinition, indexWriter: IndexWriter, regionId: Short, executor: ExecutorService) = {
    new ResourceSearcherImpl(rd, indexWriter, regionId, executor)
  }
}
