// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.config

import javax.xml.bind.annotation._

import monad.core.config.{LogFileSupport, ZkClientConfigSupport}
import monad.face.config.{ExtjsSupport, GroupConfigSupport}
import stark.utils.services.WebServerConfigSupport

/**
 * monad的组配置，通常用于某一个地方，譬如：南昌
 * @author jcai
 */
@XmlRootElement(name = "monad_group")
@XmlType(name = "MonadGroupConfig")
@XmlAccessorType(value = XmlAccessType.FIELD)
class MonadGroupConfig
  extends GroupConfigSupport
  with WebServerConfigSupport
  with LogFileSupport
  with ExtjsSupport
  with ZkClientConfigSupport
