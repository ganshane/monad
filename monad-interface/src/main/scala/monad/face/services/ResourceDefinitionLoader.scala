// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import roar.api.meta.ResourceDefinition


/**
  * 加载所有资源定义
 *
 * @author jcai
 * @see ResourceDefinition
 * @see ResourceDefinitionLoaderListener
 */
trait ResourceDefinitionLoader {
  /**
   * 得到所有的资源定义
 *
   * @return 资源定义集合
   */
  def getResourceDefinitions: Iterator[ResourceDefinition]

  /**
   * 得到某一个资源定义
 *
   * @param name 资源名称
   * @return 某一个资源定义
   */
  def getResourceDefinition(name: String): Option[ResourceDefinition]
}