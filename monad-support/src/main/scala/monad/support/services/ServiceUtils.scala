// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * 服务常用类
 * @author jcai
 */
object ServiceUtils {
  private final val NUM_RETRY = 3
  private final val BETWEEN_SECONDS = 20
    private lazy val logger = LoggerFactory getLogger getClass

    def runInNoThrow(fun: =>Unit){
      try {
        fun
      } catch {
        case NonFatal(e) => logger.error(e.getMessage, e)
      }
    }

  def waitUntilObjectLive[T](objName:String)(fun: => T):T={
        0 until NUM_RETRY foreach { i=>
            val value:T = fun
            if (value != null)
                return value
            logger.debug("waiting object live...")

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(BETWEEN_SECONDS))
        }
        throw new MonadException("经过%s s还未能获取[%s]实例".format(NUM_RETRY * BETWEEN_SECONDS,objName),MonadSupportErrorCode.TIMEOUT_TO_GET_OBJECT)
    }
}
