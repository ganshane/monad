// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.components

import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.internal.services.javascript.JavaScriptStackPathConstructor
import collection.JavaConversions._
import org.apache.tapestry5.annotations.{Parameter, BeginRender}
import org.apache.tapestry5.services.javascript.JavaScriptStackSource
import org.apache.tapestry5.{BindingConstants, MarkupWriter}

/**
 * 加载extjs
 * @author jcai
 */
class ExtLoader {
    @Parameter(allowNull = false, defaultPrefix = BindingConstants.LITERAL)
    private var resource:String = _
    @Inject
    private var javascriptStackPathConstructor:JavaScriptStackPathConstructor = _
    @Inject
    private var javaScriptStackSource:JavaScriptStackSource = _
    def defaultResource()={
        "js"
    }
    @BeginRender
    def renderExtjs(writer:MarkupWriter){
        val resourceType = if(resource == null) "js" else resource
        resourceType match {
            case "js" =>
                javascriptStackPathConstructor.constructPathsForJavaScriptStack("extjs").foreach(script=>{
                    writer.element("script", "type", "text/javascript", "src", script)
                    writer.end()
                })
            case "css" =>
                javaScriptStackSource.getStack("extjs").getStylesheets.foreach(css =>{
                    writer.element("link", "href", css.getURL,"rel","stylesheet","type", "text/css")
                    writer.end()
                })
        }
    }
}
