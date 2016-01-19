// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id

import com.google.protobuf.ExtensionRegistry
import monad.id.internal.IdMessageFilter.{InternalAddIdRequestFilter, InternalBatchAddIdRequestFilter, InternalGetIdLabelRequestFilter}
import monad.id.internal.{IdRpcServerListener, IdServiceImpl, IdZookeeperTemplate}
import monad.id.services.IdService
import monad.protocol.internal.InternalIdProto
import monad.rpc.services.{ProtobufExtensionRegistryConfiger, RpcServerListener, RpcServerMessageFilter, RpcServerMessageHandler}
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.ioc.{Configuration, OrderedConfiguration, ServiceBinder}

/**
 * local monad id module
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
object LocalMonadIdModule {
  def bind(binder:ServiceBinder): Unit ={
    binder.bind(classOf[IdService],classOf[IdServiceImpl]).withId("IdService")
    binder.bind(classOf[IdZookeeperTemplate]).withId("ZK")
  }
  @Contribute(classOf[RpcServerMessageHandler])
  def provideSyncMessageHandler(configuration: OrderedConfiguration[RpcServerMessageFilter]) {
    configuration.addInstance("AddIdRequest", classOf[InternalAddIdRequestFilter])
    configuration.addInstance("BatchAddIdRequest", classOf[InternalBatchAddIdRequestFilter])
    configuration.addInstance("GetIdLabelRequest", classOf[InternalGetIdLabelRequestFilter])

  }

  @Contribute(classOf[RpcServerListener])
  def setupSyncServerAddress(configuration: OrderedConfiguration[RpcServerListener]) {
    configuration.addInstance("id", classOf[IdRpcServerListener])
  }

  @Contribute(classOf[ExtensionRegistry])
  def provideProtobufCommand(configuration: Configuration[ProtobufExtensionRegistryConfiger]) {
    configuration.add(new ProtobufExtensionRegistryConfiger {
      override def config(registry: ExtensionRegistry): Unit = {
        InternalIdProto.registerAllExtensions(registry)
      }
    })
  }
}
