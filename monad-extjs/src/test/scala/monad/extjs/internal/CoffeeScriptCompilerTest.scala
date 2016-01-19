// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.extjs.internal

/*
 * Copyright 2012 The EGF IT Software Department.
 */

import org.junit.Test

/**
 * test coffeescript
 * @author jcai
 */

class CoffeeScriptCompilerTest {
    @Test
    def test_compile() {
        println(CoffeeScriptCompiler.compile("a=1"))

        val js = CoffeeScriptCompiler.compile(getClass.getResourceAsStream("/test.coffee"))
        println(js)
    }
}
