// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.services

import org.apache.tapestry5.services._
import monad.extjs.MonadExtjsConstants
import org.apache.tapestry5.json.{JSONArray, JSONObject}
import org.apache.tapestry5.internal.{EmptyEventContext, URLEventContext, InternalConstants}


/**
 * ext js event dispatcher
 * @author jcai
 */
class ExtEventDispatcher(
      componentRequestHandler:ComponentRequestHandler,
      extRequestGlobals:ExtRequestGlobals,
      linkEncoder:ExtDirectRequestDecoder) extends Dispatcher{

    override  def dispatch(request:Request, response:Response):Boolean=
    {
        if(extRequestGlobals.getExtRequest == null){
            return false
        }
        val  parameters = linkEncoder.decodeExtDirectRequest(extRequestGlobals.getExtRequest)

        if (parameters == null) return false
        componentRequestHandler.handleComponentEvent(parameters)
        true
    }
}
