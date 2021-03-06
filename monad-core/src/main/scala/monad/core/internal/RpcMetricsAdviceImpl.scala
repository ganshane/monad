// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import java.net.SocketAddress

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.core.services.{MetricsService, RpcMetricsAdvice}
import stark.rpc.protocol.CommandProto.BaseCommand
import stark.rpc.services.{CommandResponse, RpcClient, RpcClientMessageHandler, RpcServerMessageHandler}
import stark.utils.services.LoggerSupport
import org.apache.tapestry5.ioc.MethodAdviceReceiver
import org.apache.tapestry5.plastic.{MethodAdvice, MethodInvocation}
import org.jboss.netty.channel._

import scala.util.control.NonFatal

/**
 * rpc module metrics advice
 */
class RpcMetricsAdviceImpl(metrics: MetricsService)
  extends RpcMetricsAdvice
  with LoggerSupport {
  private lazy val clientRequestMeter = metrics.registerMeter("stark.rpc.service.RpcClient.request")
  private lazy val successMeter = metrics.registerMeter("stark.rpc.service.RpcClient.request.success")
  private lazy val failMeter = metrics.registerMeter("stark.rpc.service.RpcClient.request.fail")

  private lazy val clientAdvice = new MethodAdvice() {

    def advise(invocation: MethodInvocation) {

      if (invocation.getMethod.getName != "writeMessage") {
        invocation.proceed()
        return
      }
      clientRequestMeter.mark()

      invocation.proceed()

      val result = invocation.getReturnValue.asInstanceOf[Option[ChannelFuture]]
      if (result.isDefined)
        successMeter.mark()
      else
        failMeter.mark()
    }
  }
  private lazy val responseMeter = metrics.registerMeter("stark.rpc.service.RpcClient.response")
  private lazy val clientHandlerAdvice = new MethodAdvice {

    override def advise(invocation: MethodInvocation): Unit = {
      if (invocation.getMethod.getName == "handle") {
        responseMeter.mark()
        val originChannel = invocation.getParameter(1).asInstanceOf[Channel]
        val channelDelegated = new DelegateChannel(originChannel) {
          override def write(message: Any): ChannelFuture = {
            clientRequestMeter.mark()
            try {
              val r = super.write(message)
              successMeter.mark()
              r
            } catch {
              case NonFatal(e) =>
                failMeter.mark()
                throw e
            }
          }

          override def write(message: Any, remoteAddress: SocketAddress): ChannelFuture = {
            clientRequestMeter.mark()
            try {
              val r = super.write(message, remoteAddress)
              successMeter.mark()
              r
            } catch {
              case NonFatal(e) =>
                failMeter.mark()
                throw e
            }
          }
        }
        invocation.setParameter(1, channelDelegated)
      }
      invocation.proceed()
    }
  }
  private lazy val serverHandlerAdvice = new MethodAdvice {
    private val metricsRequest = metrics.registerMeter("stark.rpc.service.RpcServer:Request")
    private val metricsResponse = metrics.registerMeter("stark.rpc.service.RpcServer:Response")

    override def advise(invocation: MethodInvocation): Unit = {
      if (invocation.getMethod.getName != "handle") {
        invocation.proceed()
        return
      }

      metricsRequest.mark()
      //调整parameter
      val response = invocation.getParameter(1).asInstanceOf[CommandResponse]
      val newResponse = new CommandResponse {
        override def writeMessage[T](commandRequest: BaseCommand, extension: GeneratedExtension[BaseCommand, T], value: T): ChannelFuture = {
          metricsResponse.mark()
          response.writeMessage(commandRequest, extension, value)
        }

        override def writeErrorMessage[T](commandRequest: BaseCommand, message: String): ChannelFuture = {
          metricsResponse.mark()
          response.writeErrorMessage(commandRequest,message)
        }
      }
      invocation.setParameter(1, newResponse)

      invocation.proceed()
    }
  }

  def advice(receiver: MethodAdviceReceiver) {
    if (receiver.getInterface.equals(classOf[RpcClient])) {
      debug("advice rpc client with metric")
      receiver.adviseAllMethods(clientAdvice)
    } else if (receiver.getInterface.equals(classOf[RpcClientMessageHandler])) {
      debug("advice rpc message handler with metric")
      receiver.adviseAllMethods(clientHandlerAdvice)
    } else if (receiver.getInterface.equals(classOf[RpcServerMessageHandler])) {
      debug("advice rpc server message handler with metric")
      receiver.adviseAllMethods(serverHandlerAdvice)
    }
  }

  class DelegateChannel(channel: Channel) extends Channel {

    override def getUserDefinedWritability(i: Int): Boolean = channel.getUserDefinedWritability(i)

    override def setUserDefinedWritability(i: Int, b: Boolean): Unit = channel.setUserDefinedWritability(i, b)

    override def compareTo(o: Channel): Int = channel.compareTo(o)

    override def getAttachment: AnyRef = channel.getAttachment

    override def setAttachment(attachment: scala.Any): Unit = {
      channel.setAttachment(attachment)
    }

    override def setReadable(readable: Boolean): ChannelFuture = channel.setReadable(readable)

    override def setInterestOps(interestOps: Int): ChannelFuture = channel.setInterestOps(interestOps)

    override def isWritable: Boolean = channel.isWritable

    override def isReadable: Boolean = channel.isReadable

    override def getInterestOps: Int = channel.getInterestOps

    override def getCloseFuture: ChannelFuture = channel.getCloseFuture

    override def close(): ChannelFuture = channel.close()

    override def unbind(): ChannelFuture = channel.unbind()

    override def disconnect(): ChannelFuture = channel.disconnect()

    override def connect(remoteAddress: SocketAddress): ChannelFuture = channel.connect(remoteAddress)

    override def bind(localAddress: SocketAddress): ChannelFuture = channel.bind(localAddress)

    override def write(message: scala.Any, remoteAddress: SocketAddress): ChannelFuture = channel.write(message, remoteAddress)

    override def write(message: scala.Any): ChannelFuture = channel.write(message)

    override def getRemoteAddress: SocketAddress = channel.getRemoteAddress

    override def getLocalAddress: SocketAddress = channel.getLocalAddress

    override def isConnected: Boolean = channel.isConnected

    override def isBound: Boolean = channel.isBound

    override def isOpen: Boolean = channel.isOpen

    override def getPipeline: ChannelPipeline = channel.getPipeline

    override def getConfig: ChannelConfig = channel.getConfig

    override def getParent: Channel = channel.getParent

    override def getFactory: ChannelFactory = channel.getFactory

    override def getId: Integer = channel.getId
  }

}
