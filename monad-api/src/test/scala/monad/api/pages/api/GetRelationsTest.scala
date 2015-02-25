// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.RelationService
import monad.face.model.ResourceRelation
import org.junit.Test
import org.mockito.Mockito

/**
 *
 * @author jcai
 */

class GetRelationsTest {
  @Test
  def test_getRelation() {
    val relationService = Mockito.mock(classOf[RelationService])
    val rel = new ResourceRelation.Rel
    rel.name = "name"
    rel.cnName = "cnName"
    Mockito.when(relationService.findRelations).thenReturn(List(rel).iterator)

    val getRelation = new GetRelations
    getRelation.setRelationService(relationService)

    getRelation.onActivate
    Mockito.verify(relationService).findRelations
  }
}
