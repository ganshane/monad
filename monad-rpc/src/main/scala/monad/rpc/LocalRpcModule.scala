// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc

import com.google.protobuf.ExtensionRegistry
import monad.rpc.protocol.CommandProto
import monad.rpc.services.ProtobufExtensionRegistryConfiger
import org.apache.tapestry5.ioc.Configuration
import org.apache.tapestry5.ioc.annotations.{Contribute, EagerLoad}

/**
 * local rpc module
 */
object LocalRpcModule {
  @EagerLoad
  def buildProtobufRegistroy(configruation: java.util.Collection[ProtobufExtensionRegistryConfiger]) = {
    val registry = ExtensionRegistry.newInstance()
    val it = configruation.iterator()
    while (it.hasNext)
      it.next().config(registry)

    registry
  }

  @Contribute(classOf[ExtensionRegistry])
  def provideProtobufCommand(configuration: Configuration[ProtobufExtensionRegistryConfiger]) {
    configuration.add(new ProtobufExtensionRegistryConfiger {
      override def config(registry: ExtensionRegistry): Unit = {
        CommandProto.registerAllExtensions(registry)
      }
    })
  }
}
