// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import collection.JavaConversions._
import collection.mutable.Buffer
import org.apache.tapestry5.services.javascript.{StylesheetLink, JavaScriptStack}
import monad.extjs.MonadExtjsConstants
import monad.extjs.services.ExtDirectApiResolver
import org.apache.tapestry5.ioc.internal.util.AbstractResource
import java.lang.String
import org.apache.tapestry5.services.{PageRenderLinkSource, AssetSource}
import java.io.{ByteArrayInputStream, InputStream, BufferedInputStream}
import org.apache.tapestry5.{SymbolConstants, Asset}
import org.apache.tapestry5.ioc.annotations.Symbol
import collection.mutable

/**
 *
 * @author jcai
 */

class ExtJavaScriptStack(@Symbol(SymbolConstants.PRODUCTION_MODE) proudctionMode:Boolean,
                         assetSource:AssetSource,
                         extDirectApiResolver:ExtDirectApiResolver,
                         pageRenderLinkSource:PageRenderLinkSource) extends JavaScriptStack{
    //private val link = pageRenderLinkSource.createPageRenderLink(classOf[Api])
    private val url = MonadExtjsConstants.EXTJS_FLAG+"/api"
    private val api=new Asset {
        def getResource = new AbstractResource(url) {
            def newResource(path: String) = null
            def toURL = null//new URL(url)
            override def exists=true
            override def openStream():InputStream={
                new ByteArrayInputStream(extDirectApiResolver.toJs.getBytes("UTF-8"))
            }
        }

        def toClientURL = url
    }
    private val extjs =
        if (proudctionMode)
            assetSource.getExpandedAsset("${"+MonadExtjsConstants.EXT_JS_PATH+"}/ext-all.js")
        else
            assetSource.getExpandedAsset("${"+MonadExtjsConstants.EXT_JS_PATH+"}/ext-all-debug.js")
    private val libraries = bufferAsJavaList(mutable.Buffer(
        //assetSource.getExpandedAsset("${tapestry.underscore}"),
        extjs,
        assetSource.getExpandedAsset("${"+MonadExtjsConstants.EXT_JS_PATH+"}/locale/ext-lang-zh_CN.js"),
        api
        //assetSource.getClasspathAsset("extjs/ext-all.js"),
        //assetSource.getClasspathAsset("extjs/locale/ext-lang-zh_CN.js")
    ))
    private val stylesheets = bufferAsJavaList(mutable.Buffer(
        new StylesheetLink(assetSource.getExpandedAsset("${"+MonadExtjsConstants.EXT_JS_PATH+"}/resources/css/ext-all-gray.css"))))

    def getStacks = Nil

    def getJavaScriptLibraries = libraries

    def getStylesheets = stylesheets

    def getInitialization = null
}
