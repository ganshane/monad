// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import java.net.InetSocketAddress
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReferenceArray}
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{ConcurrentHashMap, Executors, ThreadFactory, TimeUnit}

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import monad.protocol.internal.CommandProto.BaseCommand
import monad.rpc.model.RpcServerLocation
import monad.rpc.services.{ProtobufCommandHelper, RpcClient, RpcClientMessageHandler, RpcServerFinder}
import monad.support.services.{LoggerSupport, ServiceWaitingInitSupport}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.{NioClientSocketChannelFactory, NioWorkerPool}

/**
 * implements netty rpc client using netty framework
 */
class NettyRpcClientImpl(val handler: RpcClientMessageHandler,
                         val registry: ExtensionRegistry,
                         val rpcServerFinder: RpcServerFinder)
  extends RpcClient
  with NettyProtobufPipelineSupport
  with ProtobufCommandHelper
  with ServiceWaitingInitSupport
  with LoggerSupport {
  private val executor = Executors.newFixedThreadPool(6, new ThreadFactory {
    private val seq = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val thread = new Thread(r)
      thread.setName("rpc-client %s".format(seq.incrementAndGet()))

      thread
    }
  })
  private val channels = new ConcurrentHashMap[RpcServerLocation, ClientChannelGroup]
  private var channelFactory: NioClientSocketChannelFactory = _
  private[internal] var bootstrap: ClientBootstrap = _

  override def writeMessage[T](serverPath: String, extension: GeneratedExtension[BaseCommand, T], value: T): Option[ChannelFuture] = {
    writeMessage(serverPath, wrap(extension, value))
  }

  override def writeMessage(serverPath: String, message: BaseCommand): Option[ChannelFuture] = {
    val serverLocationOpt = rpcServerFinder.find(serverPath)
    serverLocationOpt match {
      case Some(serverLocation) =>
        writeMessage(serverLocation, message)
      case None =>
        None
    }
  }

  override def writeMessage(serverLocation: RpcServerLocation, message: BaseCommand): Option[ChannelFuture] = {
    var channelGroup = channels.get(serverLocation)

    if (channelGroup == null) {
      channels.putIfAbsent(serverLocation, new ClientChannelGroup(serverLocation))
      channelGroup = channels.get(serverLocation)
    }
    channelGroup.writeMessage(message)
  }

  /**
   * 启动服务
   */
  override def start(): Unit = {
    throwExceptionIfServiceInitialized()

    channelFactory = new NioClientSocketChannelFactory(executor, 1, new NioWorkerPool(executor, 5))

    bootstrap = new ClientBootstrap(channelFactory)
    // config
    // @see org.jboss.netty.channel.socket.SocketChannelConfig
    bootstrap.setOption("keepAlive", true)
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("connectTimeoutMillis", 10000)
    //val executionHandler = new ExecutionHandler(executor,false,true)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline = {
        val pipeline = Channels.pipeline()
        InitPipeline(pipeline)
        pipeline.addLast("handler", new CommandClientHandler)
        pipeline
      }
    })

    serviceInitialized()
  }

  /**
   * 服务关闭
   */
  override def shutdown(): Unit = {
    closeClientChannels()
    channelFactory.releaseExternalResources()
    executor.shutdown()
  }

  private def closeClientChannels() {
    val it = channels.values().iterator()
    while (it.hasNext)
      it.next().close()
  }

  override protected def extentionRegistry: ExtensionRegistry = registry

  private class CommandClientHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
      val command = e.getMessage.asInstanceOf[BaseCommand]
      if (!handler.handle(command, e.getChannel)) {
        warn("message not handled " + command.toString)
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
      warn("exception found", e.getCause)
      //客户端发生异常，则关闭此channel
      ctx.getChannel.close()
    }
  }

  private class ClientChannelGroup(serverLocation: RpcServerLocation) {
    private val requestSeq = new AtomicLong(0)
    private val channelCount = serverLocation.channelCount
    private val oneServerChannels = new AtomicReferenceArray[Option[Channel]](serverLocation.channelCount)
    private val locker = new ReentrantLock()

    def writeMessage(any: AnyRef): Option[ChannelFuture] = {
      val channelSeq: Int = (requestSeq.incrementAndGet() % channelCount).toInt
      var channel = oneServerChannels.get(channelSeq)

      if (channel == null || channel.isEmpty)
        channel = findOrCreateChannel(channelSeq)

      if (channel.isEmpty) {
        warn("fail to create channel")
        None
      }
      else
        Some(channel.get.write(any))
    }

    private def findOrCreateChannel(channelSeq: Int): Option[Channel] = {
      try {
        locker.lock()
        var channel = oneServerChannels.get(channelSeq)
        if (channel != null && channel.isDefined) return channel

        info("creating socket for seq:{} serverLocation:{}", channelSeq, serverLocation)
        //等待bootstrap初始化
        awaitServiceInit()

        val channelFuture = bootstrap.connect(new InetSocketAddress(serverLocation.host, serverLocation.port))
        channelFuture.await(serverLocation.connectTimeoutInMillis, TimeUnit.MILLISECONDS) //等待5s进行连接

        if (!channelFuture.isSuccess) {
          error("fail to connect " + serverLocation, channelFuture.getCause)
          return None
        }
        channel = Some(channelFuture.getChannel) //等待连接
        oneServerChannels.set(channelSeq, channel)
        channel.get.getCloseFuture.addListener(new ChannelFutureListener {
          override def operationComplete(future: ChannelFuture): Unit = {
            info("closing channel {}", serverLocation)
            oneServerChannels.set(channelSeq, null)
          }
        })

        channel
      } finally {
        locker.unlock()
      }
    }

    def close() {
      0 until oneServerChannels.length() foreach {
        case i =>
          val channel = oneServerChannels.get(i)
          if (channel != null && channel.isDefined) {
            channel.get.close().awaitUninterruptibly()
          }
      }
    }
  }

}
