// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.atomic.AtomicInteger

import org.apache.lucene.index.AtomicReaderContext
import org.apache.lucene.search.{DocIdSet, DocIdSetIterator, Filter}
import org.apache.lucene.util.Bits

/**
 * support to limit search max doc.
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
private[internal] class SizeLimitedFilter(val limit: Int) extends Filter {
  private val size: AtomicInteger = new AtomicInteger(0)

  def getDocIdSet(context: AtomicReaderContext, acceptDocs: Bits) = new SizeLimitedDocIdSet

  class SizeLimitedDocIdSet extends DocIdSet {
    def iterator() = new SizeLimitedDocIdSetIterator

    //不要缓存
    override def isCacheable = false

    class SizeLimitedDocIdSetIterator extends DocIdSetIterator {
      private val current: AtomicInteger = new AtomicInteger(-1)

      def cost() = 0L

      def docID() = current.get()

      def nextDoc(): Int = {
        if (size.incrementAndGet() > limit) {
          return DocIdSetIterator.NO_MORE_DOCS
        }
        current.incrementAndGet()
      }

      def advance(target: Int): Int = {
        /*
        if (target < current.get)
            if(size.incrementAndGet() > limit){
                return DocIdSetIterator.NO_MORE_DOCS
            }
         */

        current.set(target)

        target
      }
    }

  }

}
