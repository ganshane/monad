// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.group.pages

import monad.extjs.annotations.ExtDirectMethod
import monad.extjs.model.ExtStreamResponse
import monad.face.model.ResourceRelation
import monad.group.internal.MonadGroupManager
import monad.support.services.{MonadException, XmlLoader}
import org.apache.tapestry5.ioc.annotations.Inject
import org.slf4j.LoggerFactory

/**
 * dynamic action
 * @author jcai
 */

class RelationAction {
  private val logger = LoggerFactory getLogger getClass
  @Inject
  private var monadGroupManager: MonadGroupManager = _

  @ExtDirectMethod
  def getRelation = {
    new ExtStreamResponse(monadGroupManager.getRelation)
  }

  @ExtDirectMethod
  def create(xml: String) = {
    logger.debug("xml:\n{}", xml)
    try {
      val resourceRelation = XmlLoader.parseXML[ResourceRelation](xml)
      monadGroupManager.saveOrUpdateRelation(resourceRelation, Some(xml))
    } catch {
      case e: Throwable =>
        throw MonadException.wrap(e)
    }
    new ExtStreamResponse
  }
}
