// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.services


/**
 * global request
 * @author jcai
 */
trait ExtRequestGlobals {
    /**
     * 得到ext的request
     */
    def getExtRequest:ExtRequest

    /**
     * 保存ExtRequest
     */
    def storeExtRequest(extRequest:ExtRequest)
}
