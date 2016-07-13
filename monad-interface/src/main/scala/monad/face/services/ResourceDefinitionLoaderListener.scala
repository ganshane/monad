// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import roar.api.meta.ResourceDefinition


/**
  * 资源加载的监听器.
 * 提供在资源进行加载时候的一些操作。
 *
 * @author jcai
 * @see ResourceDefinitionLoader
 */
trait ResourceDefinitionLoaderListener {
  /**
   * 当资源加载完毕的时候
 *
   * @param rd 资源定义类
   */
  def onResourceLoaded(rd: ResourceDefinition, version: Int)

  /**
   * 删除操作
 *
   * @param resourceName 资源名称
   */
  def onRemove(resourceName: String)

  /**
   * 当资源卸载的时候的请求
 *
   * @param resourceName 资源名称
   */
  def onResourceUnloaded(resourceName: String)
}
