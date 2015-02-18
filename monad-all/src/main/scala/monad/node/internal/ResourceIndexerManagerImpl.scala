// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.internal

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import com.google.gson.JsonObject
import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.dsl.Disruptor
import monad.core.services.{StartAtOnce, CronScheduleWithStartModel, LogExceptionHandler}
import monad.face.annotation.Rpc
import monad.face.config.IndexConfigSupport
import monad.face.model._
import monad.face.services.{GroupZookeeperTemplate, DocumentSource, ResourceSearcherSource}
import monad.jni.{NoSQLOptions, NodeIdNoSQL}
import monad.node.services.{ResourceIndexer, ResourceIndexerManager}
import monad.support.MonadSupportConstants
import monad.support.services.MonadException
import org.apache.lucene.store.RateLimiter
import org.apache.lucene.store.RateLimiter.SimpleRateLimiter
import org.apache.tapestry5.ioc.services.cron.{PeriodicExecutor, PeriodicJob}
import org.slf4j.LoggerFactory

/**
 * 实现资源索引管理
 * @author jcai
 */
class ResourceIndexerManagerImpl(indexConfig: IndexConfigSupport,
                                 documentSource: DocumentSource,
                                 searcherSource: ResourceSearcherSource,
                                 groupZookeeper: GroupZookeeperTemplate,
                                 periodicExecutor: PeriodicExecutor
                                  )
  extends ResourceIndexerManager {
  private val logger = LoggerFactory getLogger getClass
  //针对数据的检测
  private val EVENT_FACTORY = new EventFactory[IndexEvent] {
    def newInstance() = new IndexEvent()
  }
  private val buffer = 1 << 5
  //采用单线程进行索引操作
  private val disruptor = new Disruptor[IndexEvent](EVENT_FACTORY, buffer, Executors.newFixedThreadPool(1))
  private val searchExecutor = Executors.newFixedThreadPool(indexConfig.index.queryThread, new ThreadFactory {
    private val seq = new AtomicInteger(0)

    def newThread(p1: Runnable) = {
      val t = new Thread(p1)
      t.setName("search-%s".format(seq.incrementAndGet()))

      t
    }
  })
  @volatile
  private var running = false
  private var job: PeriodicJob = _
  private var rateLimiter: Option[RateLimiter] = None

  if (indexConfig.index.maxBytesPerSec > 0) {
    rateLimiter = Some(new SimpleRateLimiter(indexConfig.index.maxBytesPerSec / 1024.0 / 1024.0))
  }
  private var idNoSQL: Option[NodeIdNoSQL] = None

  /**
   * 启动对象实例
   */
  def start() {
    //lock memory
    if (System.getProperty("mlockall", "false") == "true")
      MemoryLocker.lockMemory()

    disruptor.handleExceptionsWith(new LogExceptionHandler)
    disruptor.handleEventsWith(new CreateDocumentHandler(this, documentSource))
    disruptor.start()

    TimeOutCollector.start()

    //启动id数据库
    if (indexConfig.index.idNoSql != null) {
      val nosqlOptions = new NoSQLOptions()
      nosqlOptions.setCache_size_mb(indexConfig.index.idNoSql.cache)
      nosqlOptions.setWrite_buffer_mb(indexConfig.index.idNoSql.writeBuffer)
      nosqlOptions.setMax_open_files(indexConfig.index.idNoSql.maxOpenFiles)
      nosqlOptions.setLog_keeped_num(indexConfig.index.binlogLength)
      try {
        idNoSQL = Some(new NodeIdNoSQL(indexConfig.index.idNoSql.path, nosqlOptions))
      } finally {
        nosqlOptions.delete()
      }
    }
    //定时更新数据
    val jobName = "monad-indexer"
    job = periodicExecutor.addJob(new CronScheduleWithStartModel("0 * * * * ? *", StartAtOnce),
      jobName, new Runnable {
        def run() {
          val oldName = Thread.currentThread().getName
          Thread.currentThread().setName(jobName)
          try {
            SyncAndIndex()
          } finally {
            if (oldName != null)
              Thread.currentThread().setName(oldName)
          }
        }
      })
    running = true
  }

  def SyncAndIndex() {
    logger.info("begin to sync {}", objects.keySet())
    val it = objects.keySet().iterator()
    while (it.hasNext && running) {
      val key = it.next()
      try {
        directGetObject(key).asInstanceOf[ResourceIndexerImpl].DoSyncData()
        if (running)
          directGetObject(key).asInstanceOf[ResourceIndexerImpl].index()
      } catch {
        case e: MonadException =>
          logger.error(e.toString)
        case e: Throwable =>
          logger.error(e.toString, e)
      }
    }
    logger.info("finish to sync all resource ,running :{}", running)
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    logger.info("closing resource index manager ....")
    running = false
    TimeOutCollector.shutdown()
    disruptor.shutdown()
    if (idNoSQL.isDefined)
      idNoSQL.get.delete()
  }

  def getDisruptor = disruptor

  def getRateLimiter = rateLimiter

  def setRegionInfo(name: String, jsonObject: JsonObject) {
    groupZookeeper.setRegionIndexInfo(name, indexConfig.regionId, jsonObject)
  }

  /**
   * search index with index name and keyword
   */
  def collectSearch(resourceName: String, q: String, sort: String, topN: Int) = {
    directGetObject(resourceName).getResourceSearcher.collectSearch(q, sort, topN)

  }

  def collectSearch2(resourceName: String, q: String, sort: String, topN: Int) = {
    directGetObject(resourceName).getResourceSearcher.collectSearch2(q, sort, topN)

  }

  //------ search

  override def facetSearch(resourceName: String, q: String, field: String, upper: Int, lower: Int): ShardResult = {
    directGetObject(resourceName).getResourceSearcher.facetSearch(q, field, upper, lower)
  }

  @Rpc(mode = "all", merge = "collectMaxDoc")
  def maxDoc(resourceName: String) = {
    directGetObject(resourceName).getResourceSearcher.maxDoc
  }

  /**
   * 搜索对象
   * @param resourceName 资源名称
   * @param q 搜索条件
   * @return 搜索比中结果
   */
  def searchObjectId(resourceName: String, q: String) = {
    directGetObject(resourceName).getResourceSearcher.searchObjectId(q)
  }

  def findObject(serverId: Short, resourceName: String, key: Array[Byte]): Option[Array[Byte]] = {
    //TODO 服务器做校验
    directGetObject(resourceName).findObject(key)
  }

  /**
   * 通过服务器的ID和资源名称，以及id序列，来查找对象的ID值
   * @param serverId 服务器ID
   * @param idSeq id序列
   * @return id的值
   */
  def findObjectId(serverId: Short, idSeq: Int) = {
    if (idNoSQL.isDefined) {
      val value = idNoSQL.get.GetId(idSeq)
      if (value == null) None else Some(value)
    } else {
      None
    }
  }

  def findObjectIdSeq(id: String): Option[IdSeqShardResult] = {
    if (idNoSQL.isDefined) {
      val value = idNoSQL.get.GetIdSeq(id.getBytes(MonadSupportConstants.UTF8_ENCODING_CHARSET))
      if (value > 0) Some(new IdSeqShardResult(value, indexConfig.regionId)) else None
    } else {
      None
    }
  }

  protected def createObject(rd: ResourceDefinition, version: Int) = {
    if (ResourceType.Data == rd.resourceType) null
    else
      new ResourceIndexerImpl(rd, indexConfig, searcherSource, this, version, searchExecutor, idNoSQL)
  }

  override protected def afterObjectRemoved(obj: ResourceIndexer) {
    obj.removeIndex()
  }
}
