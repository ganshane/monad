// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingDeque}

/**
 * timeout map
 */
trait TimeoutMap[K, V] {
  def containsKey(key: K): Boolean

  def get(key: K): Option[V]

  def putHead(key: K, value: V)

  def put(key: K, value: V)

  def remove(key: K): Option[V]

  def size: Int
}

abstract trait ExpiredCallback[K, V] {
  def expire(key: K, value: V)
}

/**
 * RotatingMap must be used under thread-safe environment
 *
 * Expires keys that have not been updated in the configured number of seconds.
 * The algorithm used will take between expirationSecs and expirationSecs * (1 +
 * 1 / (numBuckets-1)) to actually expire the message.
 *
 * get, put, remove, containsKey, and size take O(numBuckets) time to run.
 *
 */
object RotatingMap {
  val DEFAULT_NUM_BUCKETS: Int = 3
}

class RotatingMap[K, V] extends TimeoutMap[K, V] {
  private val flag = new AtomicBoolean(false)
  private var _buckets: util.Deque[java.util.Map[K, V]] = null
  private var _callback: ExpiredCallback[K, V] = null

  def this(numBuckets: Int, callback: ExpiredCallback[K, V], isSingleThread: Boolean) {
    this()
    if (numBuckets < 2) {
      throw new IllegalArgumentException("numBuckets must be >= 2")
    }
    if (isSingleThread) {
      _buckets = new util.LinkedList[java.util.Map[K, V]]
    }
    else {
      _buckets = new LinkedBlockingDeque[java.util.Map[K, V]]
    }

    0 until numBuckets foreach { case i =>
      _buckets.add(new ConcurrentHashMap[K, V])
    }
    _callback = callback
  }

  def this(callback: ExpiredCallback[K, V]) {
    this(RotatingMap.DEFAULT_NUM_BUCKETS, callback, false)
  }

  def this(numBuckets: Int) {
    this(numBuckets, null, false)
  }

  def rotate: Option[java.util.Map[K, V]] = {
    if (flag.compareAndSet(false, true)) {
      try {
        val dead: java.util.Map[K, V] = _buckets.removeLast()
        _buckets.addFirst(new ConcurrentHashMap[K, V])
        if (_callback != null) {
          val it = dead.entrySet().iterator()
          while (it.hasNext) {
            val entry = it.next()
            _callback.expire(entry.getKey, entry.getValue)
          }
        }
        return Some(dead)
      } finally {
        flag.set(false)
      }
    }
    None
  }

  def containsKey(key: K): Boolean = {
    val it = _buckets.iterator()
    while (it.hasNext) {
      val bucket = it.next()
      if (bucket.containsKey(key))
        return true
    }
    false
  }

  def get(key: K): Option[V] = {
    val it = _buckets.iterator()
    while (it.hasNext) {
      val bucket = it.next()
      if (bucket.containsKey(key))
        return Some(bucket.get(key))
    }
    None
  }

  def putHead(key: K, value: V) {
    _buckets.peekFirst.put(key, value)
  }

  def put(key: K, value: V) {
    val it = _buckets.iterator
    var bucket = it.next
    bucket.put(key, value)
    while (it.hasNext) {
      bucket = it.next
      bucket.remove(key)
    }
  }

  /**
   * Remove item from Rotate
   *
   * On the side of performance, scanning from header is faster On the side of
   * logic, it should scan from the end to first.
   *
   * @param key
   * @return
   */
  def remove(key: K): Option[V] = {
    val it = _buckets.iterator()
    while (it.hasNext) {
      val bucket = it.next()
      val value = bucket.remove(key)
      if (value != null)
        return Some(value)
    }
    None
  }

  def size: Int = {
    var size: Int = 0
    val it = _buckets.iterator()
    while (it.hasNext) {
      size += it.next().size()
    }
    size
  }
}
