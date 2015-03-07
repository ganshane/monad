// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

import monad.face.model.{ShardResult, ShardResultCollect}
import monad.protocol.internal.InternalMaxdocQueryProto.MaxdocQueryResponse
import monad.protocol.internal.InternalSearchProto.InternalSearchResponse
import monad.rpc.protocol.CommandProto
import monad.rpc.protocol.CommandProto.BaseCommand
import monad.rpc.services._
import monad.support.services.{CodingHelper, LoggerSupport}
import org.jboss.netty.channel.Channel

import scala.collection.mutable.ListBuffer

/**
 * api message filter
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-26
 */
object ApiMessageFilter {
  def createCollectSearchMerger(): RpcClientMerger[ShardResult] = new CollectSearchMerger


  def createMaxdocMerger: RpcClientMerger[Long] = new MaxdocMerger

  private class CollectSearchMerger extends RpcClientMerger[ShardResult] with LoggerSupport {
    private val list = new CopyOnWriteArrayList[ShardResult]()
    private val collect = new ShardResultCollect

    /**
     * 处理接受的消息
     */
    override def handle(commandRequest: BaseCommand, channel: Channel): Unit = {
      if (!commandRequest.hasExtension(InternalSearchResponse.cmd)) {
        error("invalid message {}", commandRequest)
      }
      val response = commandRequest.getExtension(InternalSearchResponse.cmd)
      val shardResult = new ShardResult
      shardResult.maxDoc = response.getMaxdoc
      shardResult.maxScore = response.getMaxScore
      shardResult.totalRecord = response.getTotal
      shardResult.serverHash = response.getPartitionId.toShort
      //TODO 现在兼容老的API，需要调整为新的API
      val buffer = new ListBuffer[(Array[Byte], AnyVal)]()
      val it = response.getResultsList.iterator()
      while (it.hasNext) {
        val r = it.next
        buffer.append((CodingHelper.EncodeInt32WithBigEndian(r.getId), r.getScore))
      }

      shardResult.results = buffer.toArray

      list.add(shardResult)
    }

    /**
     * 得到merge之后的结果
     * @return merge之后的结果
     */
    override def get: ShardResult = {
      collect.shardResults = list.toArray(new Array[ShardResult](list.size()))

      collect
    }
  }

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
