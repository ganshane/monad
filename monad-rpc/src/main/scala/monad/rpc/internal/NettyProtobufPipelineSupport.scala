// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import com.google.protobuf.{ExtensionRegistry, MessageLite}
import monad.rpc.protocol.CommandProto
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelHandler.Sharable
import org.jboss.netty.channel.{Channel, ChannelHandlerContext, ChannelPipeline}
import org.jboss.netty.handler.codec.oneone.{OneToOneDecoder, OneToOneEncoder}
import org.jboss.netty.handler.codec.protobuf.{ProtobufVarint32FrameDecoder, ProtobufVarint32LengthFieldPrepender}
import org.xerial.snappy.Snappy

/**
 * netty protocol pipeline support
 */
trait NettyProtobufPipelineSupport {
  protected def extentionRegistry: ExtensionRegistry

  protected def InitPipeline(pipeline: ChannelPipeline) {
    //解码
    pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder())
    //构造函数传递要解码成的类型
    pipeline.addLast("protobufDecoder", new ProtobufDecoderWithSnappy(CommandProto.BaseCommand.getDefaultInstance, extentionRegistry))
    //编码
    pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender())
    pipeline.addLast("protobufEncoder", new ProtobufEncoderWithSnappy())
  }

  @Sharable
  class ProtobufDecoderWithSnappy(prototype: MessageLite, extensionRegistry: ExtensionRegistry) extends OneToOneDecoder {
    protected def decode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
      if (!msg.isInstanceOf[ChannelBuffer]) {
        return msg
      }
      val buf: ChannelBuffer = msg.asInstanceOf[ChannelBuffer]
      var offset: Int = 0
      val length: Int = buf.readableBytes
      var array: Array[Byte] = null

      if (buf.hasArray) {
        array = buf.array
        offset = buf.arrayOffset + buf.readerIndex
      } else {
        array = new Array[Byte](length)
        buf.getBytes(buf.readerIndex, array, 0, length)
        offset = 0
      }
      val resultLength = Snappy.uncompressedLength(array, offset, length)
      val result: Array[Byte] = new Array[Byte](resultLength)
      Snappy.uncompress(array, offset, length, result, 0)

      if (extensionRegistry == null) {
        prototype.getParserForType.parseFrom(result)
      }
      else {
        prototype.getParserForType.parseFrom(result, extensionRegistry)
      }
    }
  }

  class ProtobufEncoderWithSnappy extends OneToOneEncoder {
    override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
      msg match {
        case lite: MessageLite =>
          var array = lite.toByteArray
          array = Snappy.compress(array)
          return ctx.getChannel.getConfig.getBufferFactory.getBuffer(array, 0, array.length)
        case _ =>
      }

      msg match {
        case builder: MessageLite.Builder =>
          var array = builder.build.toByteArray
          array = Snappy.compress(array)
          return ctx.getChannel.getConfig.getBufferFactory.getBuffer(array, 0, array.length)
        case _ =>
      }

      msg
    }
  }

}
