// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id.config

import javax.xml.bind.annotation._

import monad.core.config._
import stark.rpc.config.RpcBindSupport

/**
 * monad id configuration
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
@XmlRootElement(name = "monad_id")
@XmlType(name = "MonadIdConfig")
class MonadIdConfig
  extends IdConfigSupport
  with LocalStoreConfigSupport
  with RpcBindSupport
  with HeartbeatConfigSupport
  with LogFileSupport
  with ZkClientConfigSupport


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdConfigSupport")
class IdConfigSupport {
  @XmlElement(name = "id")
  var id: IdConfig = new IdConfig()
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdConfig")
class IdConfig {
  @XmlElement(name = "groups_supported")
  var groups:String = _
  @XmlElement(name = "nosql")
  var noSql: NoSqlConfig = new NoSqlConfig
}
