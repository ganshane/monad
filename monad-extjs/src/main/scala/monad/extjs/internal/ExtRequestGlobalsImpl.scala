// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.ioc.annotations.Scope
import org.apache.tapestry5.ioc.ScopeConstants
import monad.extjs.services.{ExtRequest, ExtRequestGlobals}

/**
 * 储存Ext的请求变量
 * @author jcai
 */

@Scope(ScopeConstants.PERTHREAD)
class ExtRequestGlobalsImpl extends ExtRequestGlobals {
    private var extRequest:ExtRequest = _
    def getExtRequest = extRequest
    def storeExtRequest(extRequest: ExtRequest) {
        this.extRequest = extRequest
    }
}
