// Copyright 2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.services

import com.google.gson.JsonObject
import monad.support.services.ServiceLifecycle

/**
 * memcache的客户端接口类
 * @author jcai
 */
trait MemcachedClient extends ServiceLifecycle {
  /**
   * 设置cache
   * @param key 缓存的key
   * @param result 结果数据
   */
  def set(key: String, result: String)

  /**
   * 从缓存中得到数据，如果查不到缓存，则执行真正的方法，并且放入到缓存
   * @param key 缓存的Key
   * @param f 执行的方法
   * @return 结果数据
   */
  def getOrElse(key: String, f: () => JsonObject): JsonObject
}
