// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.config

import javax.xml.bind.annotation.{XmlRootElement, XmlType}

import monad.core.config.{HeartbeatConfigSupport, LocalStoreConfigSupport, LogFileSupport, ZkClientConfigSupport}
import monad.face.config.{GroupApiSupport, SyncConfigSupport}
import monad.rpc.config.RpcBindSupport

/**
 * 针对同步模块的配置
 * @author jcai
 */
@XmlRootElement(name = "monad_sync")
@XmlType(name = "MonadSyncConfig")
class MonadSyncConfig
  extends SyncConfigSupport
  with LocalStoreConfigSupport
  with RpcBindSupport
  with ZkClientConfigSupport
  with HeartbeatConfigSupport
  with LogFileSupport
  with GroupApiSupport

