// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}
import javax.annotation.PostConstruct

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.rpc.config.RpcBindSupport
import monad.rpc.protocol.CommandProto.BaseCommand
import monad.rpc.services._
import monad.support.services.{LoggerSupport, MonadException, MonadUtils}
import org.apache.tapestry5.ioc.annotations.EagerLoad
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import scala.util.control.NonFatal

/**
 * rpc server based on netty
 */
@EagerLoad
class NettyRpcServerImpl(rpcBindSupport: RpcBindSupport,
                         messageHandler: RpcServerMessageHandler,
                         listener: RpcServerListener,
                         registry: ExtensionRegistry)
  extends RpcServer
  with NettyProtobufPipelineSupport
  with ServerChannelManagerSupport
  with LoggerSupport {
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
  private val channels = new DefaultChannelGroup("rpc-server")
  private var channelFactory: NioServerSocketChannelFactory = _
  private var bootstrap: ServerBootstrap = _

  /**
   * 启动对象实例
   */
  @PostConstruct
  def start(hub: RegistryShutdownHub) {

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

    hub.addRegistryWillShutdownListener(new Runnable {
      override def run(): Unit = shutdown()
    })
  }

  private def openOnce(): Channel = {
    try {
      val bindTuple = MonadUtils.parseBind(rpcBindSupport.rpc.bind)
      val address = new InetSocketAddress("0.0.0.0", bindTuple._2)
      val channel = bootstrap.bind(address)
      channels.add(channel)
      channel
    } catch {
      case NonFatal(e) =>
        shutdown()
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
    MonadUtils.shutdownExecutor(executor, "rpc server executor service")
    listener.afterStop()
  }

  override protected def extentionRegistry: ExtensionRegistry = registry

  class CommandServerHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
      val command = e.getMessage.asInstanceOf[BaseCommand]
      val result = messageHandler.handle(command, new ServerResponse(e.getChannel))
      if (!result) {
        error("message not handled {}", command.toString)
        ctx.getChannel.close()
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
      error("rpc server exception,client:" + e.getChannel.getRemoteAddress, e.getCause)
      //服务器上发生异常，则关闭此channel
      e.getChannel.close()
    }
  }

}

class ServerResponse(channel: Channel)
  extends CommandResponse
  with ProtobufCommandHelper {

  override def writeMessage[T](baseCommand: BaseCommand, extension: GeneratedExtension[BaseCommand, T], value: T) = {
    channel.write(wrap(baseCommand.getTaskId, extension, value))
  }
}
