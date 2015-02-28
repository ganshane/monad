// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.services

import org.apache.tapestry5.ioc.annotations.UsesMappedConfiguration

/**
 * file system asset alias manager
 * @author jcai
 */
@UsesMappedConfiguration(classOf[String])
trait FileSystemAssetAliasManager {
    /**
     * Takes a resource path to a classpath resource and adds the asset path prefix to the path. May also convert part
     * of the path to an alias (based on the manager's configuration).
     *
     * @param resourcePath
     *            resource path on the classpath (with no leading slash)
     * @return URL ready to send to the client
     */
    def toClientURL(resourcePath:String):String

    def getMappings:Map[String, String]
}
