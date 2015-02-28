// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import javax.naming.SizeLimitExceededException

import org.apache.lucene.index.AtomicReaderContext
import org.apache.lucene.search.{Collector, Scorer}

/**
 * 对结果进行限制的收集器
 * @author jcai
 */
class ResultLimitCollector(delegate: Collector, limit: Int) extends Collector {
  private var total = 0

  def resetTotal() {
    total = 0
  }

  def setScorer(scorer: Scorer) {
    delegate.setScorer(scorer)
  }

  def collect(doc: Int) {
    checkLimit();
    delegate.collect(doc)
  }

  private def checkLimit() {
    total += 1
    if (total > limit) throw new SizeLimitExceededException
  }

  def setNextReader(context: AtomicReaderContext) {
    delegate.setNextReader(context)
  }

  def acceptsDocsOutOfOrder() = delegate.acceptsDocsOutOfOrder()
}

