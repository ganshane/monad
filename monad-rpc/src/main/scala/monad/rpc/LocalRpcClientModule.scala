// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc

import com.google.protobuf.ExtensionRegistry
import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.internal.NettyRpcClientImpl
import monad.rpc.model.RpcServerLocation
import monad.rpc.services._
import org.apache.tapestry5.ioc.OrderedConfiguration
import org.apache.tapestry5.ioc.annotations.{Local, ServiceId}
import org.apache.tapestry5.ioc.services.{PipelineBuilder, RegistryShutdownHub}
import org.jboss.netty.channel.Channel
import org.slf4j.Logger

/**
 * local rpc module
 */
object LocalRpcClientModule {
  def buildRpcServerFinder(): RpcServerFinder = {
    new RpcServerFinder {
      /**
       * find rpc server
       * @param path server path
       * @return
       */
      override def find(path: String): Option[RpcServerLocation] = {
        throw new UnsupportedOperationException
      }
    }
  }

  @ServiceId("NettyRpcClient")
  def buildNettyRpcClient(handler: RpcClientMessageHandler, registry: ExtensionRegistry, rpcServerFinder: RpcServerFinder): RpcClient = {
    new NettyRpcClientImpl(handler, registry, rpcServerFinder)
  }

  def contributeRegistryStartup(configuration: OrderedConfiguration[Runnable],
                                @Local rpcClient: RpcClient,
                                registryShutdownHub: RegistryShutdownHub) {
    rpcClient.start()
    registryShutdownHub.addRegistryWillShutdownListener(new Runnable {
      def run() {
        rpcClient.shutdown()
      }
    })
  }

  @ServiceId("RpcClientMessageHandler")
  def buildRpcClientMessageHandler(pipelineBuilder: PipelineBuilder, logger: Logger,
                                   configuration: java.util.List[RpcClientMessageFilter])
  : RpcClientMessageHandler = {
    val terminator = new RpcClientMessageHandler {

      /**
       * whether block message
       * @param commandRequest message command
       * @return handled if true .
       */
      override def handle(commandRequest: BaseCommand, channel: Channel): Boolean = false
    }
    pipelineBuilder.build(logger, classOf[RpcClientMessageHandler], classOf[RpcClientMessageFilter], configuration, terminator)
  }
}
