// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.config

import javax.xml.bind.annotation.XmlRootElement

import monad.core.config._
import monad.face.config
import monad.face.config.{GroupApiSupport, DicPathSupport, ApiConfigSupport}

/**
 * api configuration
 * @author jcai
 * @version 0.1
 */
@XmlRootElement(name = "monad_api")
class MonadApiConfig
  extends config.LogFileSupport
  with GroupApiSupport
  with DicPathSupport
  with ApiConfigSupport

