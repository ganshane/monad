// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.{ChannelHandlerContext, ChannelPipeline, ChannelStateEvent, SimpleChannelUpstreamHandler}

/**
 * server channel manager
 */
trait ServerChannelManagerSupport {
  private val channels = new DefaultChannelGroup("rpc-server")

  protected def initChannelManager(pipeline: ChannelPipeline) {
    pipeline.addFirst("monitor", new MonitorChannelHandler)
  }

  protected def closeAllChannels() {
    channels.close().awaitUninterruptibly()
  }

  private class MonitorChannelHandler extends SimpleChannelUpstreamHandler {
    override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
      channels.add(e.getChannel)
    }
  }

}
