// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.pages

import org.apache.tapestry5.util.TextStreamResponse
import org.apache.tapestry5.ioc.annotations.Inject
import monad.extjs.services.ExtDirectApiResolver

/**
 * extjs api
 * @author jcai
 */
class Api {
    @Inject
    private var resolver:ExtDirectApiResolver = _
    def onActivate={
        new TextStreamResponse("application/json",resolver.toJs)
    }
}
