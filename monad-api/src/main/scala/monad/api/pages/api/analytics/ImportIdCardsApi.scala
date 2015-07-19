// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api.analytics

import javax.inject.Inject

import monad.face.MonadFaceConstants
import monad.face.internal.MonadSparseFixedBitSet
import monad.face.model.IdShardResult
import monad.face.services.IdFacade
import org.apache.tapestry5.services.Request

/**
 * 导入身份证号码
 * @author jcai
 */
class ImportIdCardsApi {
  @Inject
  private var request: Request = _
  @Inject
  private var idFacade: IdFacade = _

  def onActivate():IdShardResult = {
    val category = request.getParameter("c");
    val labels = request.getParameter("q").split(",")
    val bitSet = new MonadSparseFixedBitSet(labels.length)
    val ids = idFacade.batchAddId(category,labels)
    ids.foreach(x=> if(x!=MonadFaceConstants.UNKNOWN_ID_SEQ) bitSet.set(x))

    val collect = new IdShardResult()
    collect.data = bitSet

    collect
  }
}
