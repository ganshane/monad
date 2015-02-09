// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.services

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.protocol.internal.CommandProto.BaseCommand
import monad.support.services.ServiceLifecycle

/**
 * rpc server
 */
trait RpcServer extends ServiceLifecycle {
}

trait RpcServerListener {
  def afterStart()

  def afterStop()
}

trait CommandResponse {
  def writeMessage[T](extension: GeneratedExtension[BaseCommand, T], value: T)
}

trait RpcServerMessageHandler {
  /**
   * @param commandRequest message command
   * @return handled if true .
   */
  def handle(commandRequest: BaseCommand, response: CommandResponse): Boolean
}

trait RpcServerMessageFilter {
  def handle(commandRequest: BaseCommand,
             response: CommandResponse,
             handler: RpcServerMessageHandler): Boolean
}
