// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.config

import javax.xml.bind.annotation.XmlRootElement

import monad.core.config.{HeartbeatConfigSupport, LocalStoreConfigSupport, LogFileSupport}
import monad.face.config.{ApiConfigSupport, DicPathSupport, GroupApiSupport}
import stark.utils.services.WebServerConfigSupport

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

