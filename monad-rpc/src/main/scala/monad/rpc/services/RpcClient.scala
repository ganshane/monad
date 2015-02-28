// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.services

import java.util.concurrent.Future

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.model.RpcServerLocation
import org.apache.tapestry5.ioc.annotations.UsesOrderedConfiguration
import org.jboss.netty.channel.{Channel, ChannelFuture}

/**
 * rpc client
 */
trait RpcClient {

  /**
   * send message to remote server
   * @param serverPath server path in cloud
   * @param message message will be sent
   * @return send result future
   */
  def writeMessage(serverPath: String, message: BaseCommand): Option[ChannelFuture]

  def writeMessageWithChannel[T](channel: Channel, extension: GeneratedExtension[BaseCommand, T], value: T): Option[ChannelFuture]

  def writeMessageWithChannel(channel: Channel, message: BaseCommand): Option[ChannelFuture]
  def writeMessage[T](serverPath: String, extension: GeneratedExtension[BaseCommand, T], value: T): Option[ChannelFuture]

  def writeMessageWithBlocking[T](serverPath: String, extension: GeneratedExtension[BaseCommand, T], value: T): Future[BaseCommand]

  def writeMessageToMultiServer[T, R](serverPathPrefix: String, merger: RpcClientMerger[R],
                                      extension: GeneratedExtension[BaseCommand, T], value: T): Future[R]
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

  /**
   * 查找多个服务器，以pathPrefix开头
   * @param pathPrefix 路径开头
   * @return 服务器地址
   */
  def findMulti(pathPrefix: String): Array[RpcServerLocation]
}

/**
 * 多个服务器请求的时候进行操作的类
 * 通常该类的实现，应该是每次请求新建一个
 */
trait RpcClientMerger[T] {
  /**
   * 处理接受的消息
   */
  def handle(commandRequest: BaseCommand, channel: Channel)

  /**
   * 得到merge之后的结果
   * @return merge之后的结果
   */
  def get: T
}