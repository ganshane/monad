// Copyright 2012,2013,2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud

import monad.cloud.internal.CloudServerImpl
import monad.cloud.services.CloudServer
import org.apache.tapestry5.ioc.ServiceBinder

/**
 * local cloud module
 * @author jcai
 */
object LocalCloudModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[CloudServer], classOf[CloudServerImpl]).withId("CloudServer")
  }
}
