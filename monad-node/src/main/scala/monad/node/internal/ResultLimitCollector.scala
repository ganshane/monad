// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import javax.naming.SizeLimitExceededException

import org.apache.lucene.index.LeafReaderContext
import org.apache.lucene.search.{Collector, Scorer, SimpleCollector}

/**
 * 对结果进行限制的收集器
 * @author jcai
 */
class ResultLimitCollector(delegate: Collector, limit: Int) extends SimpleCollector {

  private var total = 0
  private var context: LeafReaderContext = _

  def resetTotal() {
    total = 0
  }

  override def doSetNextReader(context: LeafReaderContext): Unit = {
    this.context = context
  }

  override def setScorer(scorer: Scorer) {
    delegate.getLeafCollector(context).setScorer(scorer)
  }

  def collect(doc: Int) {
    checkLimit();
    delegate.getLeafCollector(context).collect(doc)
  }

  private def checkLimit() {
    total += 1
    if (total > limit) throw new SizeLimitExceededException
  }
}

