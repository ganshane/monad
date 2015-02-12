package monad.node.internal

import java.util.concurrent.TimeUnit

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap

/**
 * 针对搜索结果的cache操作
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
object SearchResultCache {

  final val EXPIRED_TIME = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)
  val cache = new ConcurrentLinkedHashMap.Builder[String, CacheEntity]()
    .maximumWeightedCapacity(2000)
    .build()

  def getOrPut[T](key: String)(function: => T): T = {
    val currentTime = TimeOutCollector.getCurrentTime
    var entity = cache.get(key)
    if (entity != null) {
      if (entity.expiredTime < currentTime) {
        //已经过期了
        entity.value = function
        entity.expiredTime = currentTime + EXPIRED_TIME
      }
      return entity.value.asInstanceOf[T]
    }
    entity = new CacheEntity(key, function, currentTime + EXPIRED_TIME)
    cache.put(key, entity)

    entity.value.asInstanceOf[T]
  }

  class CacheEntity(val key: String, var value: Any, var expiredTime: Long)

}
