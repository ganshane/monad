// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.pages.api

import monad.api.services.RelationService
import monad.face.model.ResourceRelation
import org.easymock.EasyMock._
import org.junit.Test

/**
 *
 * @author jcai
 */

class GetRelationsTest {
  @Test
  def test_getRelation() {
    val relationService = createMock(classOf[RelationService])
    val rel = new ResourceRelation.Rel
    rel.name = "name"
    rel.cnName = "cnName"
    expect(relationService.findRelations).andReturn(List(rel).iterator)

    replay(relationService)
    val getRelation = new GetRelations
    getRelation.setRelationService(relationService)

    getRelation.onActivate
    verify(relationService)
  }
}
