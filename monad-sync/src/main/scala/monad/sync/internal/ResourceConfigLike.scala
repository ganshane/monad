// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import monad.face.config.SyncConfigSupport
import monad.face.model.ResourceDefinition
import monad.face.model.ResourceDefinition.ResourceProperty

/**
 * 资源配置接口
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-19
 */
trait ResourceConfigLike extends SyncConfigLike {

  /**
   * 得到资源配置定义
   * @return 资源配置定义
   */
  def resourceDefinition: ResourceDefinition

  /**
   * 增量字段定义
   * @return 增量字段定义
   */
  def incrementColumn: ResourceProperty
}

trait SyncConfigLike {

  /**
   * 得到全局的同步配置定义
   * @return 同步配置定义
   */
  def config: SyncConfigSupport
}
