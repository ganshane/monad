// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.ioc.Resource
import org.apache.tapestry5.services.{AssetPathConverter, ClasspathAssetAliasManager, AssetFactory}
import org.apache.tapestry5.Asset
import org.apache.tapestry5.internal.services.{AbstractAsset, ResourceDigestManager}
import monad.extjs.services.FileSystemAssetAliasManager

/**
 * 实现文件系统的AssetFactory
 * @author jcai
 */

class FileSystemAssetFactory(
    digestManager:ResourceDigestManager ,
    aliasManager:FileSystemAssetAliasManager,
    converter:AssetPathConverter ) extends AssetFactory{
    val rootResource = new FileSystemResource("/")
    val invariant = converter.isInvariant

    def getRootResource = rootResource

    private def clientURL(resource:Resource):String=
    {
        val defaultPath = buildDefaultPath(resource)

        converter.convertAssetPath(defaultPath)
    }

    private def buildDefaultPath(resource:Resource):String=
    {
        val requiresDigest = digestManager.requiresDigest(resource)

        var path = resource.getPath

        if (requiresDigest)
        {
            // Resources with extensions go from foo/bar/baz.txt --> foo/bar/baz.CHECKSUM.txt

            val lastdotx = path.lastIndexOf('.')

            path = path.substring(0, lastdotx + 1) + digestManager.getDigest(resource) + path.substring(lastdotx)
        }

        aliasManager.toClientURL(path)
    }

    def createAsset(resource:Resource):Asset=
    {
        if (invariant)
        {
            return createInvariantAsset(resource)
        }

        createVariantAsset(resource)
    }

    /**
     * A variant asset must pass the resource through clientURL() all the time; very inefficient.
     */
    private def createVariantAsset(resource:Resource)=
    {
        new AbstractAsset(false)
        {
            def getResource = resource

            def toClientURL = clientURL(resource)
        }
    }

    /**
     * An invariant asset is normal, and only needs to compute the clientURL for the resource once.
     */
    private def createInvariantAsset(resource:Resource)=
    {
        new AbstractAsset(true)
        {
            private var cu:String = null

            def getResource:Resource=
            {
                resource
            }

            def toClientURL =synchronized
            {
                if (cu == null)
                {
                    cu = clientURL(resource)
                }

                cu
            }
        }
    }

}
