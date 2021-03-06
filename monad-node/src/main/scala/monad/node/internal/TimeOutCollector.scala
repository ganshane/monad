// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import monad.node.services.MonadNodeExceptionCode
import stark.utils.services.{StarkException, ServiceLifecycle}
import org.apache.lucene.index.LeafReaderContext
import org.apache.lucene.search.{Collector, Scorer, SimpleCollector}

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

class TimeOutCollector(delegate: Collector, timeoutSeconds: Int = 10) extends SimpleCollector {
  private val maxTime: Long = TimeOutCollector.timer.getNanoTime + (timeoutSeconds * 1000000000L)
  private var context: LeafReaderContext = _

  override def setScorer(scorer: Scorer) {
    delegate.getLeafCollector(context).setScorer(scorer)
  }

  override def doSetNextReader(context: LeafReaderContext): Unit = {
    this.context = context
  }

  def collect(doc: Int) {
    checkTimeout();
    delegate.getLeafCollector(context).collect(doc)
  }

  private def checkTimeout() {
    if (TimeOutCollector.timer.getNanoTime > maxTime) throw new StarkException("查询超时",
      MonadNodeExceptionCode.QUERY_TIMEOUT
    )
  }

  override def needsScores(): Boolean = delegate.needsScores()
}


