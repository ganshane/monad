// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.config.RpcBindSupport
import monad.rpc.services._
import monad.support.services.{LoggerSupport, MonadException, MonadUtils}
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

/**
 * rpc server based on netty
 */
class NettyRpcServerImpl(rpcBindSupport: RpcBindSupport,
                         messageHandler: RpcServerMessageHandler,
                         listener: RpcServerListener,
                         registry: ExtensionRegistry)
  extends RpcServer
  with NettyProtobufPipelineSupport
  with ServerChannelManagerSupport
  with LoggerSupport {
  private val channels = new DefaultChannelGroup("rpc-server")
  private var channelFactory: NioServerSocketChannelFactory = _
  private var bootstrap: ServerBootstrap = _

  /**
   * 启动对象实例
   */
  def start() {
    //一个主IO，2个worker
    val ioThread = rpcBindSupport.rpc.ioThread
    val workerThread = rpcBindSupport.rpc.workerThread
    val executor = Executors.newFixedThreadPool(ioThread + workerThread, new ThreadFactory {
      private val seq = new AtomicInteger(0)

      override def newThread(r: Runnable): Thread = {
        val thread = new Thread(r)
        thread.setName("rpc-server-%s".format(seq.incrementAndGet()))
        thread.setDaemon(true)

        thread
      }
    })

    channelFactory = new NioServerSocketChannelFactory(executor, executor, workerThread)
    bootstrap = new ServerBootstrap(channelFactory)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline: ChannelPipeline = {
        val pipeline = Channels.pipeline()
        initChannelManager(pipeline)
        InitPipeline(pipeline)
        //业务逻辑处理
        pipeline.addLast("handler", new CommandServerHandler)
        pipeline
      }
    })
    openOnce()
    listener.afterStart()
  }

  private def openOnce(): Channel = {
    try {
      val bindTuple = MonadUtils.parseBind(rpcBindSupport.rpc.bind)
      val address = new InetSocketAddress("0.0.0.0", bindTuple._2)
      val channel = bootstrap.bind(address)
      channels.add(channel)
      channel
    } catch {
      case e: Throwable =>
        throw MonadException.wrap(e)
    }
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    closeAllChannels()
    channels.close().awaitUninterruptibly()
    channelFactory.releaseExternalResources()
    listener.afterStop()
  }

  override protected def extentionRegistry: ExtensionRegistry = registry

  class CommandServerHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
      val command = e.getMessage.asInstanceOf[BaseCommand]
      val result = messageHandler.handle(command, new ServerResponse(e.getChannel))
      if (!result)
        error("message not found {}", command.toString)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
      error("exception on rpc server", e.getCause)
      //服务器上发生异常，则关闭此channel
      e.getChannel.close()
    }
  }

}

class ServerResponse(channel: Channel)
  extends CommandResponse
  with ProtobufCommandHelper {
  override def writeMessage[T](extension: GeneratedExtension[BaseCommand, T], value: T): Unit = {
    channel.write(wrap(extension, value))
  }
}
