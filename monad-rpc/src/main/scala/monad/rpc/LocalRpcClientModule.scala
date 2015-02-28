// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc

import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.internal.NettyRpcClientImpl
import monad.rpc.model.RpcServerLocation
import monad.rpc.services._
import org.apache.tapestry5.ioc.ServiceBinder
import org.apache.tapestry5.ioc.annotations.ServiceId
import org.apache.tapestry5.ioc.services.PipelineBuilder
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

      override def findMulti(pathPrefix: String): Array[RpcServerLocation] = {
        throw new UnsupportedOperationException
      }
    }
  }

  def bind(binder: ServiceBinder): Unit = {
    binder.bind(classOf[RpcClient], classOf[NettyRpcClientImpl]).withId("RpcClient")
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
