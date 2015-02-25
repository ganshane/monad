// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlRootElement}

import monad.core.config.{HeartbeatConfigSupport, LogFileSupport, ZkClientConfigSupport}
import monad.face.config.IndexConfigSupport
import monad.rpc.config.RpcBindSupport

/**
 * 节点配置类
 * @author jcai
 */
@XmlRootElement(name = "monad_node")
@XmlAccessorType(XmlAccessType.FIELD)
class MonadNodeConfig
  extends IndexConfigSupport
  with RpcBindSupport
  with LogFileSupport
  with HeartbeatConfigSupport
  with ZkClientConfigSupport
