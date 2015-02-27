package monad.api.internal

import java.util.concurrent.atomic.AtomicLong

import monad.protocol.internal.CommandProto.BaseCommand
import monad.protocol.internal.InternalMaxdocQueryProto.MaxdocQueryResponse
import monad.rpc.services._
import org.jboss.netty.channel.Channel

/**
 * api message filter
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-26
 */
object ApiMessageFilter {

  def createMaxdocMerger:RpcClientMerger[Long] = new MaxdocMerger

  private class MaxdocMerger extends RpcClientMerger[Long] {
    private val maxdoc = new AtomicLong(0)

    /**
     * 对获得的消息进行处理
     */
    override def handle(command: BaseCommand, channel: Channel): Unit = {
      val maxdocResponse = command.getExtension(MaxdocQueryResponse.cmd)
      maxdoc.addAndGet(maxdocResponse.getMaxdoc)
    }

    override def get: Long = maxdoc.get()
  }

}
