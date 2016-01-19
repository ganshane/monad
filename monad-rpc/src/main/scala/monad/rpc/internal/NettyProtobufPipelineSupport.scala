// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import com.google.protobuf.{ExtensionRegistry, MessageLite}
import monad.rpc.MonadRpcConstants
import monad.rpc.protocol.CommandProto
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelHandler.Sharable
import org.jboss.netty.channel.{Channel, ChannelHandlerContext, ChannelPipeline}
import org.jboss.netty.handler.codec.frame.{LengthFieldBasedFrameDecoder, LengthFieldPrepender}
import org.jboss.netty.handler.codec.oneone.{OneToOneDecoder, OneToOneEncoder}
import org.jboss.netty.handler.codec.protobuf.{ProtobufDecoder, ProtobufEncoder}
import org.xerial.snappy.Snappy

/**
 * netty protocol pipeline support
 */
trait NettyProtobufPipelineSupport {
  protected def extentionRegistry: ExtensionRegistry
  private val MAX_FRAME_LENGTH = 10 * 1024 * 1024

  private val commpressSupported =
    System.getProperty(MonadRpcConstants.RPC_COMPRESS_SUPPORTED, "true") != "false"

  protected def InitPipeline(pipeline: ChannelPipeline,maxBuffer:Int=MAX_FRAME_LENGTH) {
    //解码
    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(maxBuffer,0,4,0,4))
    //构造函数传递要解码成的类型
    if (commpressSupported)
      pipeline.addLast("protobufDecoder", new ProtobufDecoderWithSnappy(CommandProto.BaseCommand.getDefaultInstance, extentionRegistry))
    else
      pipeline.addLast("protobufDecoder", new ProtobufDecoder(CommandProto.BaseCommand.getDefaultInstance, extentionRegistry))
    //编码
    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4))

    if (commpressSupported)
      pipeline.addLast("protobufEncoder", new ProtobufEncoderWithSnappy())
    else
      pipeline.addLast("protobufEncoder", new ProtobufEncoder())
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
    private def compressMessage(messageLite: MessageLite, ctx: ChannelHandlerContext): ChannelBuffer = {
      var array = messageLite.toByteArray
      array = Snappy.compress(array)
      ctx.getChannel.getConfig.getBufferFactory.getBuffer(array, 0, array.length)
    }

    override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef) = msg match {
      case lite: MessageLite =>
        compressMessage(lite, ctx)
      case builder: MessageLite.Builder =>
        compressMessage(builder.build(), ctx)
      case _ =>
        msg

    }
  }

}
