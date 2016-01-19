// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.services

import com.google.gson.JsonArray

/**
 *
 * @author jcai
 */
trait ExtRequest {
    /** request type type **/
    def getType:String
    /** action **/
    def getAction:String
    /** method **/
    def getMethod:String
    /** transaction id **/
    def getTid:String
    /** data **/
    def getData:JsonArray
}
