// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlRootElement}

import monad.core.config.{HeartbeatConfigSupport, LocalStoreConfigSupport, LogFileSupport, ZkClientConfigSupport}
import monad.face.config.{GroupApiSupport, IndexConfigSupport}
import monad.rpc.config.RpcBindSupport

/**
 * 节点配置类
 * @author jcai
 */
@XmlRootElement(name = "monad_node")
@XmlAccessorType(XmlAccessType.FIELD)
class MonadNodeConfig
  extends IndexConfigSupport
  with LocalStoreConfigSupport
  with RpcBindSupport
  with LogFileSupport
  with HeartbeatConfigSupport
  with ZkClientConfigSupport
  with GroupApiSupport
