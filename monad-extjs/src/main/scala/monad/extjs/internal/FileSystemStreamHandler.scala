// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.services.assets.AssetRequestHandler
import org.apache.tapestry5.services.{Response, Request}
import java.lang.String
import org.apache.tapestry5.internal.services.{AssetResourceLocator, ResourceStreamer}

/**
 *
 * @author jcai
 */

class FileSystemStreamHandler(streamer:ResourceStreamer,
                              assetResourceLocator:AssetResourceLocator,
                              baseFolder:String) extends AssetRequestHandler{
    def handleAssetRequest(request: Request, response: Response, extraPath: String):Boolean = {
        val assetPath = "file:"+baseFolder + "/" + extraPath

        val resource = assetResourceLocator.findClasspathResourceForPath(assetPath)

        if (resource == null)
            return false

        streamer.streamResource(resource)

        true
    }
}
