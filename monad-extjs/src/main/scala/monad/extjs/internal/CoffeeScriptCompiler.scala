// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.mozilla.javascript.{ContextFactory, ScriptableObject, Context}
import java.io.{InputStream, InputStreamReader}
import org.slf4j.LoggerFactory
import io.Source
import java.util.concurrent.locks.ReentrantLock

/**
 * CoffeeScript compiler
 * @author jcai
 */

object CoffeeScriptCompiler {
    private val classLoader = getClass.getClassLoader
    private val inputStream = classLoader.getResourceAsStream("coffee-script.js")
    private var globalScope: ScriptableObject = _
    private val logger = LoggerFactory getLogger getClass
    private val locker = new ReentrantLock()
    private val contextFactory = new ContextFactory();

  try {
        val reader = new InputStreamReader(inputStream, "UTF-8")
        try {
            val context = contextFactory.enterContext()
            try {

                globalScope = context.initStandardObjects()
                context.setOptimizationLevel(-1); // Without this, Rhino hits a 64K bytecode limit and fails

                context.evaluateReader(globalScope, reader, "coffee-script.js", 1, null)
            } finally {
                Context.exit()
            }
        } finally {
            reader.close()
        }
    } finally {
        inputStream.close()
    }

    def compile(coffeeScriptSource: InputStream): String = {
        compile(Source.fromInputStream(coffeeScriptSource, "UTF-8").mkString)
        /*
        val reader = new InputStreamReader(coffeeScriptSource,"UTF-8")
        val ba=Stream.continually(reader.read).takeWhile(-1 !=).map(_.toByte).toArray
        compile(new String(ba,"UTF-8"))
        */
    }

    def compile(coffeeScriptSource: String): String = {
        try{
            locker.lock()
            if (logger.isDebugEnabled) {
                logger.debug("script:\n{}", coffeeScriptSource)
            }
            val context = contextFactory.enterContext();

            try {
                val compileScope = context.newObject(globalScope)
                compileScope.setParentScope(globalScope)
                compileScope.put("coffeeScriptSource", compileScope, coffeeScriptSource)
                context.evaluateString(compileScope,
                    String.format("CoffeeScript.compile(coffeeScriptSource, %s);", "{bare:true}"),
                    "CoffeeScriptCompiler", 1, null).asInstanceOf[String]
            } finally {
                Context.exit()
            }

        }finally {
            locker.unlock()
        }
    }
}
