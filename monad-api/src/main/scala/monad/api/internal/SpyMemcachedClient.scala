// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai.
 * site: http://www.ganshane.com
 */

package monad.api.internal

import java.util.concurrent.TimeUnit

import com.google.gson.JsonObject
import monad.api.MonadApiConstants
import monad.api.services.MemcachedClient
import net.spy.memcached.{AddrUtil, OperationTimeoutException}
import org.slf4j.LoggerFactory

/**
 * memcached client
 * @author jcai
 */
class SpyMemcachedClient(enabled: Boolean, addresses: String, expiredInMinutes: Int) extends MemcachedClient {
  //build builder
  private lazy val memcachedClient = buildClient
  //logger
  private val logger = LoggerFactory getLogger getClass

  /**
   * 启动对象实例
   */
  def start() {

  }

  /**
   * 关闭对象
   */
  def shutdown() {
    if (memcachedClient != null) {
      memcachedClient.shutdown()
    }
  }

  /**
   * get object  or execute common function to get
   */
  def getOrElse(key: String, f: () => JsonObject): JsonObject = {
    if (memcachedClient == null)
      return f()
    var value: String = null
    try {
      value = memcachedClient.get(key).asInstanceOf[String]
      if (value == null) {
        value = f().toString
        set(key, value)
      }
    } catch {
      case e: OperationTimeoutException =>
        logger.warn("从memcached获取数据超时,{}", e.getMessage)
        value = f().toString
        set(key, value)
    }

    MonadApiConstants.JSON_PARSER.parse(value).getAsJsonObject
  }

  /**
   * put SearchResult to memcache server
   */
  def set(key: String, result: String) {
    if (memcachedClient != null)
      memcachedClient.set(key, TimeUnit.MINUTES.toSeconds(expiredInMinutes).toInt, result)
  }

  private def buildClient = {
    if (enabled) {
      //new net.spy.memcached.MemcachedClient(new BinaryConnectionFactory(),
      //    AddrUtil.getAddresses(config.api.memcachedServers));
      new net.spy.memcached.MemcachedClient(AddrUtil.getAddresses(addresses))

      //using xmemcached client
      /* val builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(config.api.memcachedServers))
      builder.build();
      */
    } else {
      null
    }
  }
}
