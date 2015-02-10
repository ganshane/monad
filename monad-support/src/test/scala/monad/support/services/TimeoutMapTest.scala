// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

import org.junit.{Assert, Test}

/**
 * Created by jcai on 14-8-17.
 */
class TimeoutMapTest extends LoggerSupport {
  @Test
  def test_concurrent() {
    val pool = Executors.newCachedThreadPool()
    val map = new RotatingMap[String, String](3)
    val latch = new CountDownLatch(10)
    @volatile
    var lastRotate = System.currentTimeMillis()
    val rotateTime = 1000L * (10 / 3)
    val r = new AtomicInteger(0)
    val runnable = new Runnable {
      override def run(): Unit = {
        latch.countDown()
        latch.await()
        debug("OK")
        try {

          0 until 1000 foreach { case j =>
            map.put(j.toString, "j:" + j)
            // println("put "+j)
            LockSupport.parkNanos(1000 * 1000 * 20)
            //移除超时的数据
            val now: Long = System.currentTimeMillis
            if (now - lastRotate > rotateTime) {
              lastRotate = now
              val tmp = map.rotate
              val size = if (tmp.isDefined) tmp.get.size else 0
              info("Acker's timeout item size:{}", size)
              r.addAndGet(size)
            }
          }
        } catch {
          case e: Throwable =>
            error("ex", e)
        }
        debug("finish loop")
      }
    }

    0 until 10 foreach { case i =>
      pool.submit(runnable)
    }
    pool.shutdown()
    pool.awaitTermination(100, TimeUnit.MINUTES)
    r.addAndGet(map.size)
    Assert.assertEquals(1000, r.get())
  }
}
