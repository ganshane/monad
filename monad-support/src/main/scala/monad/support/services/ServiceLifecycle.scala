// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

/**
 * service lifecycle
 */
trait ServiceLifecycle {
  /**
   * 启动服务
   */
  def start()

  /**
   * 服务关闭
   */
  def shutdown()
}
