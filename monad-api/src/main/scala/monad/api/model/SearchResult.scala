// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.model

import com.google.gson.JsonArray
import monad.face.model.ShardResult
import org.apache.lucene.util.PriorityQueue
import org.slf4j.LoggerFactory

/**
 * 搜索结果
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.3
 */
object SearchResult {
  private val logger = LoggerFactory getLogger getClass

  /*
  def mergeFacet(field: String, shardHits: Array[ShardResult]): SearchResult = {
    val result = new ObjectObjectOpenHashMap[String, SfzhStat]
    var count = 0
    var shardRow: (Array[Byte], AnyVal) = null
    var all = 0
    for (i <- 0 until shardHits.length) {
      val shard = shardHits(i)
      all += shard.maxDoc

      for (j <- 0 until shard.totalRecord) {
        shardRow = shard.results(j)
        count = shardRow._2.asInstanceOf[Int]
        logger.debug("objectId:{}", shardRow._1)
        val facet = shard.facetArr(j)
        var value = result.get(facet)
        if (value == null) {
          value = new SfzhStat(-1, count)
          value.serverId = shard.serverHash
          value.objectId = shardRow._1
          result.put(facet, value)
        } else {
          value.increaseCount(count)
        }
      }
      shard.results = null
    }


    val topN = math.min(100, result.size())
    //初始化排序队列
    val pq = new PriorityQueue[SfzhStat](topN) {
      def lessThan(a: SfzhStat, b: SfzhStat) = a.count < b.count
    }
    result.forEach(new ObjectObjectProcedure[String, SfzhStat] {
      def apply(key: String, stat: SfzhStat) {
        //val stat = value.asInstanceOf[SfzhStat]
        stat.sfzh = key.asInstanceOf[String]
        logger.debug("insert stat:{}", stat)

        pq.insertWithOverflow(stat)
      }
    })

    val total = pq.size
    val hits = new Array[Array[Byte]](total)
    val faceCount = new Array[Int](total)
    val servers = new Array[Short](total)
    for (i <- total.until(0, -1)) {
      val stat = pq.pop()
      hits(i - 1) = stat.objectId
      servers(i - 1) = stat.serverId
      faceCount(i - 1) = stat.count
    }
    new SearchResult(total, hits, servers, all, Some(faceCount))

  }
  */

  def merge(start: Int, offset: Int, shardHits: Array[ShardResult]): SearchResult = {
    val queue: PriorityQueue[ShardRef] = new ScoreMergeSortQueue(shardHits)
    //var maxScore: Float = java.lang.Float.MIN_VALUE
    var totalHitCount = 0
    var all: Int = 0
    for (shardIDX <- 0 until shardHits.length) {
      val shard = shardHits(shardIDX)
      all += shard.maxDoc //当返回的结果总数为0，总数则进行增加操作
      if (shard.totalRecord > 0 && shard.results.length > 0) {
        totalHitCount += shard.totalRecord
        queue.add(new ShardRef(shardIDX))
        //maxScore = math.max(maxScore, shard.maxScore)
        if (logger.isDebugEnabled) {
          logger.debug("shard:{},num:{}", shardIDX, shard.results.length)
        }
      }
    }
    //最大需要遍历的位置
    val maxHit = math.min(start + offset, totalHitCount)
    //避免start过高造成错误
    if (maxHit < start) {
      return new SearchResult(totalHitCount, Array[Array[Byte]](), Array[Short](), all)
    }
    //结果数组
    val hits = new Array[Array[Byte]](maxHit - start)
    val servers = new Array[Short](hits.length)
    //游标变量
    var hitUpto: Int = 0
    while (hitUpto < maxHit) {
      assert(queue.size > 0)
      val ref: ShardRef = queue.pop
      val shardResult = shardHits(ref.shardIndex)

      //在范围内，则记录该值到结果集中
      if (start <= hitUpto) {
        val hit = shardResult.results(ref.hitIndex)
        hits(hitUpto - start) = hit._1
        servers(hitUpto - start) = shardResult.serverHash
      }
      ref.hitIndex += 1
      hitUpto += 1
      //shard中的索引还未遍历完，加入队列
      if (ref.hitIndex < shardHits(ref.shardIndex).results.length) {
        queue.add(ref)
      }
    }
    new SearchResult(totalHitCount, hits, servers, all)
  }

  // Refers to one hit:
  private class ShardRef(val shardIndex: Int) {
    var hitIndex: Int = 0
  }

  private class ScoreMergeSortQueue(shardHits: Array[ShardResult]) extends PriorityQueue[ShardRef](shardHits.length) {
    //initialize(shardHits.length)


    def lessThan(first: ShardRef, second: ShardRef): Boolean = {
      assert(first ne second)
      //获得得分值
      val firstScore: Float = shardHits(first.shardIndex).results(first.hitIndex)._2.asInstanceOf[Float]
      val secondScore: Float = shardHits(second.shardIndex).results(second.hitIndex)._2.asInstanceOf[Float]

      if (firstScore < secondScore) {
        false
      }
      else if (firstScore > secondScore) {
        true
      }
      else {
        if (first.shardIndex < second.shardIndex) {
          true
        }
        else if (first.shardIndex > second.shardIndex) {
          false
        }
        else {
          assert(first.hitIndex != second.hitIndex)
          first.hitIndex < second.hitIndex
        }
      }
    }
  }

}

class SearchResult(val hitCount: Int, val hits: Array[Array[Byte]], val servers: Array[Short], val all: Int, val facetCount: Option[Array[Int]] = None) {
  var nodeAll: Int = 0
  var nodeSuccess: Int = 0
  var nodeSuccessInfo: JsonArray = _
  var nodeError: Int = 0
}
