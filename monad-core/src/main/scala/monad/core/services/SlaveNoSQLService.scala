// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import monad.jni.services.gen.SlaveNoSQLSupport

/**
 * Created by jcai on 14-8-23.
 */
trait SlaveNoSQLService extends DataSynchronizer {
  def nosql(): Option[SlaveNoSQLSupport]
}
