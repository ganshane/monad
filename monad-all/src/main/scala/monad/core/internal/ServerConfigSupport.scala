// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.internal

/**
 *
 * @author jcai
 */
trait ServerConfigSupport {
  protected var otherModules = Array[Class[_]]()
  protected var filterModules = Array[Class[_]]()
  protected var mainPackage = "monad.api"

  def setOtherModule(modules: Array[Class[_]]) {
    otherModules = modules
  }

  def setFilterModule(modules: Array[Class[_]]) {
    filterModules = modules
  }

  def setPackage(pack: String) {
    mainPackage = pack
  }
}
