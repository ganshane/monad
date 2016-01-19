// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.ioc.services.Coercion
import com.google.gson.{JsonArray, JsonParser}

/**
 * string -> JsonArray
 * @author jcai
 */

class StringToJsonArray extends Coercion[String,JsonArray]{
    def coerce(input:String)={
        new JsonParser().parse(input).getAsJsonArray
    }
}
