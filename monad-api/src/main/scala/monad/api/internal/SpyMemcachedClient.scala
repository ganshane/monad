// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai.
 * site: http://www.ganshane.com
 */

package monad.api.internal

import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

import com.google.gson.JsonObject
import monad.api.MonadApiConstants
import monad.api.services.MemcachedClient
import net.spy.memcached.{AddrUtil, OperationTimeoutException}
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.slf4j.LoggerFactory

/**
 * memcached client
 * @author jcai
 */
class SpyMemcachedClient(enabled: Boolean, addresses: String, expiredInMinutes: Int) extends MemcachedClient {
  //logger
  private val logger = LoggerFactory getLogger getClass
  //build builder
  private var memcachedClient: Option[net.spy.memcached.MemcachedClient] = None

  /**
   * 关闭对象
   */
  @PostConstruct
  def start(hub: RegistryShutdownHub) {
    memcachedClient = buildClient
    hub.addRegistryShutdownListener(new Runnable {
      override def run(): Unit = {
        memcachedClient.foreach(_.shutdown())
      }
    })
  }

  private def buildClient = {
    if (enabled) {
      Some(new net.spy.memcached.MemcachedClient(AddrUtil.getAddresses(addresses)))
    } else {
      None
    }
  }

  /**
   * get object  or execute common function to get
   */
  def getOrElse(key: String, f: () => JsonObject): JsonObject = {
    memcachedClient match {
      case Some(client) =>
        var value: String = null
        try {
          value = client.get(key).asInstanceOf[String]
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
      case None =>
        f()
    }
  }

  /**
   * put SearchResult to memcache server
   */
  def set(key: String, result: String) {
    memcachedClient match {
      case Some(client) =>
        client.set(key, TimeUnit.MINUTES.toSeconds(expiredInMinutes).toInt, result)
      case None =>
      //do nothing
    }
  }
}
