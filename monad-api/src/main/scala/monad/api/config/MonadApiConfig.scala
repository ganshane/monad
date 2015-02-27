// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.config

import javax.xml.bind.annotation.XmlRootElement

import monad.core.config.{HeartbeatConfigSupport, LocalStoreConfigSupport, LogFileSupport}
import monad.face.config.{ApiConfigSupport, DicPathSupport, GroupApiSupport}
import monad.support.services.WebServerConfigSupport

/**
 * api configuration
 * @author jcai
 * @version 0.1
 */
@XmlRootElement(name = "monad_api")
class MonadApiConfig
  extends LogFileSupport
  with GroupApiSupport
  with HeartbeatConfigSupport
  with LocalStoreConfigSupport
  with DicPathSupport
  with ApiConfigSupport
  with WebServerConfigSupport

