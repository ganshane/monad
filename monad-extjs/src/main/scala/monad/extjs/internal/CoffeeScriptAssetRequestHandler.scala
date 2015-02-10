// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.extjs.internal

import org.apache.tapestry5.internal.services.assets.ClasspathAssetRequestHandler
import org.apache.tapestry5.internal.services.{AssetResourceLocator, ResourceStreamer}
import org.apache.tapestry5.services.{Response, Request}
import org.apache.tapestry5.ioc.Resource
import java.util.Locale
import java.io.{ByteArrayInputStream, InputStream}
import org.apache.tapestry5.ioc.internal.util.InternalUtils

/**
 * 专门对CoffeeScript的处理，能够在编译时刻把coffeescript转换成js
 * @author jcai
 */
class CoffeeScriptAssetRequestHandler(streamer: ResourceStreamer,
                                      assetResourceLocator: AssetResourceLocator,
                                      baseFolder: String) extends ClasspathAssetRequestHandler(streamer, assetResourceLocator, baseFolder) {
    override def handleAssetRequest(request: Request, response: Response, extraPath: String): Boolean = {
        val assetPath = baseFolder + "/" + extraPath

        val resource = assetResourceLocator.findClasspathResourceForPath(assetPath)

        if (resource == null) {
            return false
        }

        if (assetPath.endsWith(".coffee")) {
            //如果请求.coffee文件，则把他转换成js内容
            streamer.streamResource(createJsResourceFromCoffee(resource))
        } else if (!resource.exists() && assetPath.endsWith(".js")) {
            val coffeePath = assetPath.replaceAll(".js$", ".coffee")
            val coffeeResource = assetResourceLocator.findClasspathResourceForPath(coffeePath)
            if (coffeeResource != null && coffeeResource.exists()) {
                streamer.streamResource(createJsResourceFromCoffee(coffeeResource))
            }
        } else {
            streamer.streamResource(resource)
        }
        true
    }

    private def createJsResourceFromCoffee(coffee: Resource): Resource = {
        val is = coffee.openStream()
        try {
            val jsContent = CoffeeScriptCompiler.compile(is)
            new Resource() {

                def exists() = coffee.exists()

                def openStream() = new ByteArrayInputStream(jsContent.getBytes("UTF-8"))

                def toURL = coffee.toURL

                def forLocale(locale: Locale) = coffee.forLocale(locale)

                def forFile(relativePath: String) = coffee.forFile(relativePath)

                def withExtension(extension: String) = coffee.withExtension(extension)

                def getFolder = coffee.getFolder

                def getFile = coffee.getFile

                def getPath = coffee.getPath

                override def toString = toURL.toString
            }
        } finally {
            InternalUtils.close(is)
        }
    }

}
