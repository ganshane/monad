// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.services

import com.lmax.disruptor.ExceptionHandler
import monad.support.services.MonadException
import org.slf4j.LoggerFactory

/**
 * 针对异常的处理，仅仅输出
 * @author jcai
 */
class LogExceptionHandler extends ExceptionHandler {
  private val logger = LoggerFactory getLogger getClass

  def handleEventException(ex: Throwable, sequence: Long, event: Any) {
    ex match {
      case e: MonadException =>
        logger.error(e.toString)
      case e: InterruptedException =>
      //do nothing
      case _ =>
        logger.error(ex.getMessage, ex)
    }
  }

  def handleOnStartException(ex: Throwable) {
    logger.error(ex.getMessage, ex)
  }

  def handleOnShutdownException(ex: Throwable) {
    logger.error(ex.getMessage, ex)
  }
}
