// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

/**
 * exception reporter.
 */
trait ErrorReporter {
  /**
   * 报告系统异常消息
   * @param error 系统异常消息
   */
  def report(error: Throwable)
}

