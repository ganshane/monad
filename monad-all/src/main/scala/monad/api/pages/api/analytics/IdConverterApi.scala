package monad.api.pages.api.analytics

import javax.inject.Inject

import monad.api.MonadApiConstants
import monad.face.services.RpcSearcherFacade
import monad.support.MonadSupportConstants
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
  private var idFacade: RpcSearcherFacade = _

  def onActivate() = {
    val ids = request.getParameter("q").split(",")
    val regions = request.getParameter("r").split(",")
    val sb = new StringBuilder()
    for ((id, region) <- ids.zip(regions)) {
      if (!id.isEmpty) {
        val i = idFacade.findObjectId(region.toShort, id.toInt)
        if (i.isDefined && i.get != null) {
          sb.append(new String(i.get, MonadSupportConstants.UTF8_ENCODING))
        } else {
          //便于前端进行分析对应
          sb.append("")
        }
        sb.append(",")
      }
    }
    if (sb.length > 0) {
      sb.setLength(sb.length - 1)
    }
    response.setHeader(MonadApiConstants.HEADER_ACCESS_CONTROL_ALLOW, "*")
    new TextStreamResponse("text/palin", sb.toString())
  }
}
