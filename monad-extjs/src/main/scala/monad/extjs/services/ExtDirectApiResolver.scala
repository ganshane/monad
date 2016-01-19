// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.services

import com.google.gson.JsonObject

/**
 * get all direct actions
 * @author jcai
 */
trait ExtDirectApiResolver {
    /**
     * get extjs actions as JsonObject
     */
    def getActions:JsonObject

    /**
     * 产生js内容
     */
    def toJs:String
}
