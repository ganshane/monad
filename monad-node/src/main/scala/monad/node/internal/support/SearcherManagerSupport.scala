// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal.support

import java.util.concurrent.{Semaphore, TimeUnit}

import monad.node.internal.InternalIndexSearcher
import monad.node.services.MonadNodeExceptionCode
import monad.support.services.MonadException
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.store.AlreadyClosedException

/**
 * 支持搜索管理
 * @author jcai
 */
trait SearcherManagerSupport {
  private val semaphore = new Semaphore(5)

  //全局搜索对象
  protected def getSearcherManager: SearcherManager

  protected def doInSearcher[T](fun: InternalIndexSearcher => T): T = {
    val sm = getSearcherManager
    if (semaphore.tryAcquire(60, TimeUnit.SECONDS)) {
      try {
        val s = sm.acquire().asInstanceOf[InternalIndexSearcher]
        try {
          fun(s)
        } finally {
          sm.release(s)
        }
      } catch {
        case e: AlreadyClosedException =>
          throw new MonadException("Server正在关闭", MonadNodeExceptionCode.SEARCHER_CLOSING)
      } finally {
        semaphore.release()
      }
    } else {
      throw new MonadException("并发过高，等待获取Analyzer超时",
        MonadNodeExceptionCode.HIGH_CONCURRENT
      )
    }
  }
}
