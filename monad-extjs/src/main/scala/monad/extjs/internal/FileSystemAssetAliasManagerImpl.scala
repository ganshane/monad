// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import monad.extjs.services.FileSystemAssetAliasManager
import org.apache.tapestry5.services.assets.AssetPathConstructor
import org.apache.tapestry5.ioc.internal.util.CollectionFactory
import collection.JavaConversions._
import java.util.{Comparator, Collections}
import org.apache.tapestry5.ioc.util.{AvailableValues, UnknownValueException}
import util.Sorting

/**
 * implements file system asset lias manager
 * @author jcai
 */

class FileSystemAssetAliasManagerImpl(assetPathConstructor:AssetPathConstructor,configuration:java.util.Map[String, String]) extends FileSystemAssetAliasManager{
    /**
     * Map from alias to path.
     */
    private var aliasToPathPrefix = Map[String,String]()

    /**
     * Map from path to alias.
     */
    private var pathPrefixToAlias = Map[String, String]()

    configuration.entrySet().foreach{ e=>
        val alias = verify("folder name", e.getKey)
        val path = verify("path", e.getValue)
        aliasToPathPrefix += (alias -> path)
        pathPrefixToAlias += (path -> alias)
    }
    val sortDescendingByLength = new Comparator[String]()
    {
        def compare(o1:String,o2:String )= o2.length() - o1.length()
    }

    val orderByLength = Ordering[Int].on[String](_.length)

    private val sortedAliases = aliasToPathPrefix.keySet.toList.sorted(orderByLength)
    //Collections.sort(sortedAliases, sortDescendingByLength);

    private val sortedPathPrefixes = aliasToPathPrefix.values.toList.sorted(orderByLength)

    private def verify(name:String , input:String):String=
    {

        if (input.endsWith("/"))
            throw new RuntimeException(String.format("Contribution of %s '%s' is invalid as it may not start with or end with a slash.",
                name, input))
        input
    }

    def toClientURL(resourcePath:String):String=
    {
        sortedPathPrefixes.foreach(pathPrefix=>{

            if (resourcePath.startsWith(pathPrefix))
            {
                val virtualFolder = pathPrefixToAlias.get(pathPrefix).get

                val virtualPath = resourcePath.substring(pathPrefix.length() + 1)

                return assetPathConstructor.constructAssetPath(virtualFolder, virtualPath)
            }
        })

        // This is a minor misuse of the UnknownValueException but the exception reporting
        // is too useful to pass up.

        throw new UnknownValueException(
            String.format(
                "Unable to create a client URL for file system resource %s: The resource path was not within an aliased path.",
                resourcePath), new AvailableValues("Aliased paths", aliasToPathPrefix.values))
    }

    def getMappings=aliasToPathPrefix
}
