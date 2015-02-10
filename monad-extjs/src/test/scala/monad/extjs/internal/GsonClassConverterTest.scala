// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

// Copyright 2012 The EGF IT Software Department.
// site: http://www.ganshane.com

import org.junit.{Assert, Test}
import collection.JavaConversions._
import com.google.gson.JsonObject


/**
 * gson class converter
 * @author jcai
 */
class GsonClassConverterTest {
    @Test
    def test_toJSON() {
        val obj = new ClassA
        obj.prop1 = "asdf"
        val json = GsonClassConverter.toJSON(obj)
        Assert.assertEquals("{\"prop1\":\"asdf\"}",json.toString)
        val obj2 = GsonClassConverter.fromJSON[ClassA](json.toString)
        Assert.assertEquals(obj.prop1, obj2.prop1)

        val data = List(obj)
        Assert.assertEquals("[{\"prop1\":\"asdf\"}]",GsonClassConverter.toJSON(data).toString)

        GsonClassConverter.toJSON(None)
        val r = GsonClassConverter.toJSON(new JsonObject)
        Assert.assertEquals("{}",r.toString)
        
    }
}

class ClassA {
    var prop1: String = _
}

