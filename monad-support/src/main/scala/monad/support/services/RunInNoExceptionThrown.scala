// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services


/**
 * run some function in no exception thrown
 */
trait RunInNoExceptionThrown {
  this: LoggerSupport =>
  def runInNotExceptionThrown(fun: => Unit) {
    try {
      fun
    } catch {
      case e: Throwable => error(e.getMessage, e)
    }
  }
}
