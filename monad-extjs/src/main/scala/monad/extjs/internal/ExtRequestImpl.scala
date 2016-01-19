// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import com.google.gson.JsonArray
import monad.extjs.services.ExtRequest

/**
 * ext direct 请求的封装
 * @author jcai
 */

class ExtRequestImpl extends ExtRequest{
    /** request type type **/
    var requestType:String = _
    /** action **/
    var action:String = _
    /** method **/
    var method:String = _
    /** transaction id **/
    var tid:String = _
    /** data **/
    var data:JsonArray = _

    def getType:String=requestType
    def getAction:String = action
    def getMethod:String = method
    def getTid:String = tid
    def getData:JsonArray = data
}
