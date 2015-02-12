// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.internal

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.{EventFactory, EventTranslator}
import monad.core.config.SyncConfigSupport
import monad.core.internal.AbstractResourceDefinitionLoaderListener
import monad.core.services.{GroupZookeeperTemplate, LogExceptionHandler}
import monad.face.model.{ResourceDefinition, ResourceType}
import monad.face.services.ResourceDefinitionLoader
import monad.jni.{NoSQLOptions, SyncIdNoSQL}
import monad.support.services.ServiceLifecycle
import monad.sync.model.DataEvent
import org.apache.tapestry5.ioc.ObjectLocator
import org.apache.tapestry5.ioc.services.ParallelExecutor
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor

import scala.collection.JavaConversions._

/**
 * 资源的导入
 * @author jcai
 */
class ResourceImporterManager(resourceSyncServer: ResourceSyncServer,
                              objectLocator: ObjectLocator,
                              periodicExecutor: PeriodicExecutor,
                              parallelExecutor: ParallelExecutor,
                              resourceDefinitionLoader: ResourceDefinitionLoader,
                              zk: GroupZookeeperTemplate,
                              syncConfig: SyncConfigSupport)
  extends AbstractResourceDefinitionLoaderListener[ResourceImporter]
  with ServiceLifecycle {
  private val EVENT_FACTORY = new EventFactory[DataEvent] {
    def newInstance() = new DataEvent()
  }
  private val buffer = 1 << 12
  private var disruptor: Disruptor[DataEvent] = null
  private var dbReader: ExecutorService = null
  private var idNoSQL: Option[SyncIdNoSQL] = None

  /**
   * 启动对象实例
   */
  def start() {
    dbReader = Executors.newFixedThreadPool(syncConfig.sync.db_thread_num + 1, new ThreadFactory {
      private val seq = new AtomicInteger(0)

      def newThread(p1: Runnable) = {
        val t = new Thread(p1)
        t.setName("sync-%s".format(seq.incrementAndGet()))

        t
      }
    })
    disruptor = new Disruptor[DataEvent](EVENT_FACTORY, buffer, dbReader)
    disruptor.handleExceptionsWith(new LogExceptionHandler)
    disruptor.
      handleEventsWith(objectLocator.autobuild(classOf[SaveRecordHandler]))
    disruptor.start()

    if (syncConfig.sync.idNoSql != null) {
      val nosqlOptions = new NoSQLOptions()
      nosqlOptions.setCache_size_mb(syncConfig.sync.idNoSql.cache)
      nosqlOptions.setWrite_buffer_mb(syncConfig.sync.idNoSql.writeBuffer)
      nosqlOptions.setMax_open_files(syncConfig.sync.idNoSql.maxOpenFiles)
      nosqlOptions.setLog_keeped_num(syncConfig.sync.binlogLength)
      try {
        idNoSQL = Some(new SyncIdNoSQL(syncConfig.sync.idNoSql.path, nosqlOptions))
        syncConfig.sync.nodes.foreach(x => idNoSQL.get.AddRegion(x.id))
      } finally {
        nosqlOptions.delete()
      }

    }

    //启动同步服务器
    resourceSyncServer.start()
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    dbReader.shutdownNow()
    disruptor.shutdown()
    dbReader.awaitTermination(4, TimeUnit.SECONDS)


    if (resourceSyncServer != null)
      resourceSyncServer.shutdown()
    if (idNoSQL.isDefined)
      idNoSQL.get.delete()
  }

  def importData(resourceName: String,
                 row: Array[Any],
                 timestamp: Long,
                 version: Int) {
    disruptor.publishEvent(new EventTranslator[DataEvent] {
      def translateTo(event: DataEvent, sequence: Long) {
        event.reset()
        event.resourceName = resourceName
        event.row = row
        event.timestamp = timestamp
        event.version = version
      }
    })
  }

  //当资源卸载的时候
  override def onResourceUnloaded(resourceKey: String) {
    try {
      resourceSyncServer.removeResourceSaver(resourceKey)
    } finally {
      super.onResourceUnloaded(resourceKey)
    }
  }

  def resync(resourceName: String) {
    zk.resync(resourceName)
  }

  def submitSync(fun: => Unit) = {
    dbReader.submit(new Runnable() {
      def run() {
        fun
      }
    })
  }

  /**
   * 创建对象
   */
  protected def createObject(rd: ResourceDefinition, version: Int) = {
    rd.resourceType match {
      case ResourceType.Virtual =>
        resourceSyncServer.createResourceSaverIfPresent(rd, version, idNoSQL = idNoSQL)
        null
      case other =>
        var saverDelegated: Option[String => Option[ResourceSaver]] = None
        if (other == ResourceType.Data) {
          //仅仅是资源提供者，则要得到真实的资源保存器
          saverDelegated = Some(targetSaverFinder)
        }
        val saver = resourceSyncServer.createResourceSaverIfPresent(rd, version, saverDelegated, idNoSQL)
        new ResourceImporter(rd, this, periodicExecutor, parallelExecutor, version, saver, syncConfig)
    }
  }

  def targetSaverFinder(resourceName: String): Option[ResourceSaver] = {
    resourceSyncServer.getSaver(resourceName)
  }

  override protected def afterObjectRemoved(obj: ResourceImporter) {
    resourceSyncServer.destroyResourceSaver(obj.rd.name)
  }
}
