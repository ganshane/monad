// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.internal

import monad.node.services.MonadNodeExceptionCode
import monad.support.services.{MonadException, ServiceLifecycle}
import org.apache.lucene.index.AtomicReaderContext
import org.apache.lucene.search.{Collector, Scorer}

/**
 * 支持查询超时的API
 * @author jcai
 */
object TimeOutCollector extends ServiceLifecycle {
  //计时器线程，方便记录当前操作执行的时间
  private val timer = new TimerThread


  def start() {
    timer.start()
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    timer.stopTimer()
  }

  def getCurrentTime = timer.getNanoTime

  /**
   * 计时器线程，方便对超时进行处理
   */
  private class TimerThread extends Thread("SearcherTimeout Thread") {
    this.setDaemon(true)
    private val resolution = 20
    @volatile var time: Long = 0
    private var stopFlag: Boolean = false


    override def run() {
      while (!stopFlag) {
        time = System.nanoTime()
        Thread.sleep(resolution)
      }
    }

    def stopTimer() {
      stopFlag = true
    }

    def getNanoTime = time
  }

}

class TimeOutCollector(delegate: Collector, timeoutSeconds: Int = 10) extends Collector {
  private val maxTime: Long = TimeOutCollector.timer.getNanoTime + (timeoutSeconds * 1000000000L)

  def setScorer(scorer: Scorer) {
    delegate.setScorer(scorer)
  }

  def collect(doc: Int) {
    checkTimeout();
    delegate.collect(doc)
  }

  private def checkTimeout() {
    if (TimeOutCollector.timer.getNanoTime > maxTime) throw new MonadException("查询超时",
      MonadNodeExceptionCode.QUERY_TIMEOUT
    )
  }

  def setNextReader(context: AtomicReaderContext) {
    delegate.setNextReader(context)
  }

  def acceptsDocsOutOfOrder() = delegate.acceptsDocsOutOfOrder()
}


