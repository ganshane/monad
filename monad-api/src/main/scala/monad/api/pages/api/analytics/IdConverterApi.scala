// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.pages.api.analytics

import javax.inject.Inject

import monad.api.MonadApiConstants
import monad.face.services.IdFacade
import org.apache.tapestry5.services.{Request, Response}
import org.apache.tapestry5.util.TextStreamResponse

import scala.collection.mutable.ListBuffer

/**
 * 把ID转换为真实数据
 *
 * @author jcai
 */
class IdConverterApi {
  @Inject
  private var request: Request = _
  @Inject
  private var response: Response = _
  @Inject
  private var idFacade: IdFacade = _
  private val pattern = "([\\d]+)\\@([\\d]+)".r
  def onActivate() = {
    val ids = request.getParameter("q")
    val groups = pattern.findAllMatchIn(ids)
    val list = new ListBuffer[String]()
    val category = request.getParameter("c");
    while(groups.hasNext){
      val g = groups.next()
      val objectIdStr =idFacade.findObjectId("trace",g.group(2).toInt,category,g.group(1).toInt)
      list += objectIdStr
    }
    response.setHeader(MonadApiConstants.HEADER_ACCESS_CONTROL_ALLOW, "*")
    new TextStreamResponse("text/palin", list.mkString(","))
  }
}
