// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import org.slf4j.LoggerFactory

/**
 * logger support
 */
trait LoggerSupport {
  protected val logger = LoggerFactory getLogger getClass

  protected def info(message: String, args: Any*): Unit = {
    if (logger.isInfoEnabled) {
      logger.info(message, args.map(_.asInstanceOf[AnyRef]).toArray)
    }
  }

  protected def warn(message: String, args: Any*): Unit = {
    if (logger.isWarnEnabled) {
      logger.warn(message, args.map(_.asInstanceOf[AnyRef]).toArray)
    }
  }

  protected def error(message: String, args: Any*): Unit = {
    if (logger.isErrorEnabled) {
      logger.error(message, args.map(_.asInstanceOf[AnyRef]).toArray)
    }
  }

  protected def error(message: String, throwable: Throwable): Unit = {
    if (logger.isErrorEnabled) {
      logger.error(message, throwable)
    }
  }

  protected def debug(message: String, args: Any*): Unit = {
    if (logger.isDebugEnabled) {
      logger.debug(message, args.map(_.asInstanceOf[AnyRef]).toArray)
    }
  }
}
