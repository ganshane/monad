// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc

import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.internal.NettyRpcServerImpl
import monad.rpc.services._
import org.apache.tapestry5.ioc.ServiceBinder
import org.apache.tapestry5.ioc.annotations.ServiceId
import org.apache.tapestry5.ioc.services.{ChainBuilder, PipelineBuilder}
import org.slf4j.Logger

/**
 * local rpc module
 */
object LocalRpcServerModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[RpcServer], classOf[NettyRpcServerImpl]).
      withId("RpcServer")
  }

  def buildRpcServerListener(chainBuilder: ChainBuilder, configuration: java.util.List[RpcServerListener]): RpcServerListener = {
    chainBuilder.build(classOf[RpcServerListener], configuration)
  }

  @ServiceId("RpcServerMessageHandler")
  def buildRpcServerMessageHandler(pipelineBuilder: PipelineBuilder, logger: Logger,
                                   configuration: java.util.List[RpcServerMessageFilter])
  : RpcServerMessageHandler = {
    val terminator = new RpcServerMessageHandler {
      /**
       * @param commandRequest message command
       * @return handled if true .
       */
      override def handle(commandRequest: BaseCommand, response: CommandResponse): Boolean = false
    }
    pipelineBuilder.build(logger, classOf[RpcServerMessageHandler], classOf[RpcServerMessageFilter], configuration, terminator)
  }
}
