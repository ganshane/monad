// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

import com.google.protobuf.ByteString
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.{EventFactory, EventTranslator}
import monad.core.services.LogExceptionHandler
import monad.face.config.SyncConfigSupport
import monad.face.internal.AbstractResourceDefinitionLoaderListener
import monad.face.model.ResourceDefinition
import monad.face.services.{GroupZookeeperTemplate, ResourceDefinitionLoader}
import monad.jni.services.gen.NoSQLOptions
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.support.services.{LoggerSupport, MonadUtils}
import monad.sync.model.DataEvent
import monad.sync.services.ResourceImporterManager
import org.apache.tapestry5.ioc.ObjectLocator
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor
import org.apache.tapestry5.ioc.services.{ParallelExecutor, RegistryShutdownHub}

/**
 * 资源的导入
 * @author jcai
 */
class ResourceImporterManagerImpl(objectLocator: ObjectLocator,
                                  periodicExecutor: PeriodicExecutor,
                                  parallelExecutor: ParallelExecutor,
                                  resourceDefinitionLoader: ResourceDefinitionLoader,
                                  zk: GroupZookeeperTemplate,
                                  syncConfig: SyncConfigSupport)
  extends ResourceImporterManager
  with AbstractResourceDefinitionLoaderListener[ResourceImporter]
  with LoggerSupport {
  private val EVENT_FACTORY = new EventFactory[DataEvent] {
    def newInstance() = new DataEvent()
  }
  private val buffer = 1 << 12
  private var disruptor: Disruptor[DataEvent] = null
  private var dbReader: ExecutorService = null

  /**
   * 启动对象实例
   */
  @PostConstruct
  def start(hub: RegistryShutdownHub) {
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
    }

    hub.addRegistryWillShutdownListener(new Runnable {
      override def run(): Unit = shutdown()
    })
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    debug("shutdown importer manager ...")
    try {
      disruptor.shutdown(2, TimeUnit.SECONDS)
    } catch {
      case e: Throwable =>
        disruptor.halt()
    }
    MonadUtils.shutdownExecutor(dbReader, "resource importer manager")
  }


  override def fetchSyncData(request: SyncRequest): SyncResponse = {
    val resourceName = request.getResourceName
    val importer = directGetObject(resourceName)
    if (importer == null) {
      val syncResponseBuilder = SyncResponse.newBuilder()
      syncResponseBuilder.setResourceName(resourceName)
      syncResponseBuilder.setPartitionId(request.getPartitionId)
      syncResponseBuilder.setMessage(ByteString.copyFromUtf8("resource not found"))
      syncResponseBuilder.build()
    } else {
      importer.doFetchSyncData(request).setResourceName(resourceName).build()
    }
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

  override protected def afterObjectRemoved(obj: ResourceImporter) {
    obj.destroyNoSQL()
  }

  /**
   * 创建对象
   */
  protected def createObject(rd: ResourceDefinition, version: Int) = {
    new ResourceImporter(rd, this, periodicExecutor, parallelExecutor, version, syncConfig)
  }
}
