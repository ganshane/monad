// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.ioc.services.Coercion
import com.google.gson.{JsonParser, JsonObject}

/**
 * string -> JsonObject
 * @author jcai
 */

class StringToJsonObject extends Coercion[String,JsonObject]{
    def coerce(input:String)={
        new JsonParser().parse(input).getAsJsonObject
    }
}
