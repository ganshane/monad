// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.services

import com.google.protobuf.GeneratedMessage
import monad.rpc.protocol.CommandProto
import monad.rpc.protocol.CommandProto.BaseCommand

/**
 * protocol command helper trait
 */
trait ProtobufCommandHelper {
  def wrap[T](extension: GeneratedMessage.GeneratedExtension[BaseCommand, T], value: T): BaseCommand = {
    BaseCommand.newBuilder().setExtension(extension, value).setTaskId(-1L).build()
  }

  def wrap[T](taskId: Long, extension: GeneratedMessage.GeneratedExtension[BaseCommand, T], value: T): BaseCommand = {
    BaseCommand.newBuilder().setExtension(extension, value).setTaskId(taskId).build()
  }
}
