// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.services

import monad.jni.services.gen.SlaveNoSQLSupport

/**
 * Created by jcai on 14-8-23.
 */
trait SlaveNoSQLService {
  def nosqlOpt(): Option[SlaveNoSQLSupport]
}
