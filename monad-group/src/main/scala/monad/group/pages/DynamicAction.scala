// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.group.pages

import monad.extjs.annotations.ExtDirectMethod
import monad.extjs.model.ExtStreamResponse
import monad.face.model.DynamicResourceDefinition
import monad.group.internal.MonadGroupManager
import stark.utils.services.{StarkException, XmlLoader}
import org.apache.tapestry5.ioc.annotations.Inject
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * dynamic action
 * @author jcai
 */

class DynamicAction {
  private val logger = LoggerFactory getLogger getClass
  @Inject
  private var monadGroupManager: MonadGroupManager = _

  @ExtDirectMethod
  def getDynamic = {
    new ExtStreamResponse(monadGroupManager.getDynamic)
  }

  @ExtDirectMethod
  def create(xml: String) = {
    logger.debug("xml:\n{}", xml)
    try {
      val dynamic = XmlLoader.parseXML[DynamicResourceDefinition](xml)
      monadGroupManager.saveOrUpdateDynamic(dynamic, Some(xml))
    } catch {
      case NonFatal(e) =>
        throw StarkException.wrap(e)
    }
    new ExtStreamResponse
  }
}
