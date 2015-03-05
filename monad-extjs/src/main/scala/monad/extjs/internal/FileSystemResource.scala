// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import java.io.{File, FileNotFoundException}
import java.net.URL

import org.apache.tapestry5.ioc.internal.util.AbstractResource

/**
 * file system resource
 * @author jcai
 */

class FileSystemResource(path:String) extends AbstractResource(path){
    // Guarded by this
    private var url:URL = _

    // Guarded by this
    private var urlResolved:Boolean = false
    def newResource(path: String) = new FileSystemResource('/'+path)

  def toURL = if (urlResolved) url
  else {
    urlResolved = true
    val file = new File(path)
    if (!file.exists())
      throw new FileNotFoundException(path)

    url = file.toURI.toURL
    url
  }
    override def hashCode():Int=
    {
        227 ^ path.hashCode()
    }

    override def equals(obj:Any):Boolean={
        if (this.eq(obj.asInstanceOf[AnyRef])) return true
        if (obj == null) return false
        if (getClass != obj.getClass) return false

        val other =  obj.asInstanceOf[FileSystemResource]

        path == other.getPath
    }
}
