// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.junit.Test

/**
 *
 * @author jcai
 */

class StringToJsonArrayTest {
    @Test
    def test_string_to_json() {
        val stja = new StringToJsonArray
        stja.coerce("['v1','v2']")
    }
    @Test
    def test_string_to_jsonObject(){
        val stja = new StringToJsonObject
        stja.coerce("{k1:'v1',k2:'v2'}")
    }
}
