// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.io._
import java.util.UUID
import java.util.concurrent.{ExecutorService, TimeUnit}

import org.apache.commons.io.input.ClassLoaderObjectInputStream

/**
 * monad utils
 */
object MonadUtils
  extends LoggerSupport
  with NetworkUtilsSupport {
  /**
   * 解析host:port形式的网络地址
   * @param bind 网络地址
   * @return host和port
   */
  def parseBind(bind: String): (String, Int) = {
    val arr = bind.split(":")
    (arr(0), arr(1).toInt)
  }

  /**
   * 比较两个byte数组
   * @param a 数组1
   * @param b 数组2
   * @return a是否小于b
   */
  def isLessThan(a: Array[Byte], b: Array[Byte]): Boolean = {
    val len = math.min(a.length, b.length)
    0 until len foreach {
      case i =>
        if (a.apply(i) < b.apply(i))
          return true
    }
    false
  }

  /**
   * 得到随机的64位的long数据
   * @return long数据
   */
  def secureRandomLong: Long = {
    UUID.randomUUID.getLeastSignificantBits
  }

  def currentTimeInSecs: Int = {
    (System.currentTimeMillis() / 1000).toInt
  }

  /**
   * 对象序列化
   * @param obj 对象
   * @return 对象序列化后的字节数组
   */
  def serialize(obj: AnyRef): Array[Byte] = {
    try {
      val bos: ByteArrayOutputStream = new ByteArrayOutputStream
      val oos: ObjectOutputStream = new ObjectOutputStream(bos)
      oos.writeObject(obj)
      oos.close()
      bos.toByteArray
    }
    catch {
      case ioe: IOException =>
        throw new RuntimeException(ioe)
    }
  }

  /**
   * 反序列化
   * @param serialized 序列化的数据
   * @return  对象
   */
  def deserialize(serialized: Array[Byte]): AnyRef = {
    deserialize(serialized, Thread.currentThread().getContextClassLoader)
  }

  /**
   * 通过给定的class loader来进行反序列化
   * @param serialized 序列化数据
   * @param loader class loader
   * @return 对象
   */
  def deserialize(serialized: Array[Byte], loader: ClassLoader): AnyRef = {
    try {
      val bis: ByteArrayInputStream = new ByteArrayInputStream(serialized)
      var ret: AnyRef = null
      if (loader != null) {
        val cis: ClassLoaderObjectInputStream = new ClassLoaderObjectInputStream(loader, bis)
        ret = cis.readObject
        cis.close()
      }
      else {
        val ois: ObjectInputStream = new ObjectInputStream(bis)
        ret = ois.readObject
        ois.close()
      }
      ret
    }
    catch {
      case ioe: IOException =>
        throw new RuntimeException(ioe)
      case e: ClassNotFoundException =>
        throw new RuntimeException(e)
    }
  }

  /**
   * 关闭某个executor
   * Uses the shutdown pattern from http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
   * @param executor Executor service
   * @param executorName executor name
   */
  def shutdownExecutor(executor: ExecutorService, executorName: String) {
    if (executor == null)
      return
    //停止新任务提交
    executor.shutdown()
    try {
      //等待已经执行的任务关闭
      if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
        //取消所有正在执行的任务
        executor.shutdownNow
        //再次等待任务关闭
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
          warn("executor {} not terminated", executorName)
        }
      }
    }
    catch {
      case ie: InterruptedException =>
        executor.shutdownNow
        Thread.currentThread.interrupt()
    }
  }

}
