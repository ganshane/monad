// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import javax.annotation.PostConstruct

import com.google.gson.JsonObject
import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.dsl.Disruptor
import monad.core.services.LogExceptionHandler
import monad.face.MonadFaceConstants
import monad.face.config.IndexConfigSupport
import monad.face.model._
import monad.face.services.{DocumentSource, GroupZookeeperTemplate, ResourceSearcherSource}
import monad.jni.services.gen.SlaveNoSQLSupport
import monad.node.services.{ResourceIndexer, ResourceIndexerManager}
import monad.rpc.services.RpcClient
import monad.support.services.{LoggerSupport, MonadException}
import org.apache.lucene.store.RateLimiter
import org.apache.lucene.store.RateLimiter.SimpleRateLimiter
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor

import scala.util.control.NonFatal

/**
 * 实现资源索引管理
 * @author jcai
 */
class ResourceIndexerManagerImpl(indexConfig: IndexConfigSupport,
                                 documentSource: DocumentSource,
                                 searcherSource: ResourceSearcherSource,
                                 groupZookeeper: GroupZookeeperTemplate,
                                 periodicExecutor: PeriodicExecutor,
                                 rpcClient: RpcClient
                                  )
  extends ResourceIndexerManager with DataSynchronizerSupport with LoggerSupport {
  //针对数据的检测
  private val EVENT_FACTORY = new EventFactory[IndexEvent] {
    def newInstance() = new IndexEvent()
  }
  private val buffer = 1 << 5
  //采用单线程进行索引操作
  private val disruptor = new Disruptor[IndexEvent](EVENT_FACTORY, buffer, Executors.newFixedThreadPool(1, new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setName("index-0")
      t.setDaemon(true)

      t
    }
  }))
  private val searchExecutor = Executors.newFixedThreadPool(indexConfig.index.queryThread, new ThreadFactory {
    private val seq = new AtomicInteger(0)

    def newThread(p1: Runnable) = {
      val t = new Thread(p1)
      t.setName("search-%s".format(seq.incrementAndGet()))
      t.setDaemon(true)

      t
    }
  })
  private var rateLimiter: Option[RateLimiter] = None

  if (indexConfig.index.maxBytesPerSec > 0) {
    rateLimiter = Some(new SimpleRateLimiter(indexConfig.index.maxBytesPerSec / 1024.0 / 1024.0))
  }

  /**
   * 启动对象实例
   */
  @PostConstruct
  def start(hub: RegistryShutdownHub) {
    //lock memory
    if (System.getProperty("mlockall", "false") == "true")
      MemoryLocker.lockMemory()

    disruptor.handleExceptionsWith(new LogExceptionHandler)
    disruptor.handleEventsWith(new CreateDocumentHandler(this, documentSource))
    disruptor.start()

    TimeOutCollector.start()

    startSynchronizer(MonadFaceConstants.MACHINE_SYNC, periodicExecutor, rpcClient)

    hub.addRegistryWillShutdownListener(new Runnable {
      override def run(): Unit = shutdown()
    })
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    logger.info("closing resource index manager ....")
    shutdownSynchronizer()
    TimeOutCollector.shutdown()
    try {
      disruptor.shutdown(2, TimeUnit.SECONDS)
    } catch {
      case NonFatal(e) =>
        disruptor.halt()
    }
  }

  override def getPartitionId: Short = {
    indexConfig.partitionId
  }

  override def findNoSQLByResourceName(resourceName: String): Option[SlaveNoSQLSupport] = {
    directGetObject(resourceName).flatMap(_.nosqlOpt())
  }

  override def afterFinishSync(): Unit = {
    getResourceList.foreach { r =>
      try {
        info("[{}] begin index ...", r)
        directGetObject(r) match{
          case Some(indexer) =>
            indexer.asInstanceOf[ResourceIndexerImpl].index()
          case None =>
            //do nothing
            error("["+r+" indexer not found]")
        }
        info("[{}] finish index", r)
      } catch {
        case e: MonadException =>
          logger.error(e.toString)
        case NonFatal(e) =>
          logger.error(e.toString, e)
      }
    }
  }

  override def getResourceList: Array[String] = {
    objects.keySet().toArray(new Array[String](objects.size()))
  }

  def getDisruptor = disruptor

  def getRateLimiter = rateLimiter

  def setRegionInfo(name: String, jsonObject: JsonObject) {
    groupZookeeper.setRegionIndexInfo(name, indexConfig.partitionId, jsonObject)
  }

  /**
   * search index with index name and keyword
   */
  def collectSearch(resourceName: String, q: String, sort: String, topN: Int) = {
    directGetObject(resourceName) match{
      case Some(indexer) =>
        indexer.getResourceSearcher.collectSearch(q, sort, topN)
      case None =>
        logger.error("[{}] indexer not found",resourceName)
        val shardResult = new ShardResult
        shardResult.results=Array[(Array[Byte],AnyVal)]()
        shardResult.maxDoc = 0
        shardResult.totalRecord = 0

        shardResult
    }

  }

  //------ search

  override def facetSearch(resourceName: String, q: String, field: String, upper: Int, lower: Int): ShardResult = {
    directGetObject(resourceName) match{
      case Some(indexer) =>
        indexer.getResourceSearcher.facetSearch(q, field, upper,lower)
      case None =>
        logger.error("[{}] indexer not found",resourceName)
        val shardResult = new ShardResult
        shardResult.results=Array[(Array[Byte],AnyVal)]()
        shardResult.maxDoc = 0
        shardResult.totalRecord = 0

        shardResult
    }
  }

  def maxDoc(resourceName: String) = {
    directGetObject(resourceName) match{
      case Some(indexer) =>
        indexer.getResourceSearcher.maxDoc
      case None =>
        logger.error("[{}] indexer not found",resourceName)
        0
    }
  }

  /**
   * 搜索对象
   * @param resourceName 资源名称
   * @param q 搜索条件
   * @return 搜索比中结果
   */
  def searchObjectId(resourceName: String, q: String) = {
    directGetObject(resourceName) match{
      case Some(indexer) =>
        indexer.getResourceSearcher.searchObjectId(q)
      case None =>
        logger.error("[{}] indexer not found",resourceName)
        throw new UnsupportedOperationException
    }
  }

  def findObject(serverId: Short, resourceName: String, key: Array[Byte]): Option[Array[Byte]] = {
    directGetObject(resourceName) match{
      case Some(indexer) =>
        indexer.findObject(key)
      case None =>
        logger.error("[{}] indexer not found",resourceName)
        None
    }
  }


  protected def createObject(rd: ResourceDefinition, version: Int) = {
    if (ResourceType.Data == rd.resourceType) null
    else
      new ResourceIndexerImpl(rd, indexConfig, searcherSource, this, version, searchExecutor)
  }

  override protected def afterObjectRemoved(obj: ResourceIndexer) {
    obj.removeIndex()
  }
}
