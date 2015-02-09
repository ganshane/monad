// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.services

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.model.RpcServerLocation
import monad.support.services.ServiceLifecycle
import org.apache.tapestry5.ioc.annotations.UsesOrderedConfiguration
import org.jboss.netty.channel.{Channel, ChannelFuture}

/**
 * rpc client
 */
trait RpcClient extends ServiceLifecycle {
  /**
   * send message to remote server
   * @param serverLocation remote rpc server location
   * @param message message will be sent
   * @return send future
   */
  def writeMessage(serverLocation: RpcServerLocation, message: BaseCommand): Option[ChannelFuture]

  /**
   * send message to remote server
   * @param serverPath server path in cloud
   * @param message message will be sent
   * @return send result future
   */
  def writeMessage(serverPath: String, message: BaseCommand): Option[ChannelFuture]

  def writeMessage[T](serverPath: String, extension: GeneratedExtension[BaseCommand, T], value: T): Option[ChannelFuture]
}

trait RpcClientSupport {
  protected def getRpcClient: RpcClient
}

sealed abstract class ClientRequestMode

case object ContinueRequest extends ClientRequestMode

case object StopRequest extends ClientRequestMode

/**
 * client block message sent
 */
@UsesOrderedConfiguration(classOf[RpcClientMessageFilter])
trait RpcClientMessageHandler {
  /**
   * whether block message
   * @param commandRequest message command
   * @return handled if true .
   */
  def handle(commandRequest: BaseCommand, channel: Channel): Boolean
}

trait RpcClientMessageFilter {
  /**
   * handle rpc client message
   * @param command base command
   * @return
   */
  def handle(command: BaseCommand, channel: Channel, handler: RpcClientMessageHandler): Boolean
}

/**
 * find rpc host information in cloud
 */
trait RpcServerFinder {
  /**
   * find rpc server
   * @param path server path
   * @return
   */
  def find(path: String): Option[RpcServerLocation]
}
