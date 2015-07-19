// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api.analytics

import javax.inject.Inject

import monad.api.MonadApiConstants
import monad.face.services.IdFacade
import org.apache.tapestry5.services.{Request, Response}
import org.apache.tapestry5.util.TextStreamResponse

/**
 * 把ID转换为真实数据
 * @author jcai
 */
class IdConverterApi {
  @Inject
  private var request: Request = _
  @Inject
  private var response: Response = _
  @Inject
  private var idFacade: IdFacade = _

  def onActivate() = {
    val ids = request.getParameter("q").split(",").map(_.toInt)
    val category = request.getParameter("c");
    val label = idFacade.findObjectId(category,ids)
    response.setHeader(MonadApiConstants.HEADER_ACCESS_CONTROL_ALLOW, "*")
    new TextStreamResponse("text/palin", label.mkString(","))
  }
}
