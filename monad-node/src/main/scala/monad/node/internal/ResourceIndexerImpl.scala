// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.internal

import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

import com.google.gson.{JsonObject, JsonParser}
import com.lmax.disruptor.EventTranslator
import monad.face.MonadFaceConstants
import monad.face.config.{IndexConfigSupport, ServerIdSupport}
import monad.face.model.{AnalyzerCreator, IndexEvent, ResourceDefinition}
import monad.face.services.{DataTypeUtils, ResourceSearcher, ResourceSearcherSource}
import monad.jni.services.gen.NoSQLOptions
import monad.node.services.{MonadNodeExceptionCode, ResourceIndexer, ResourceIndexerManager}
import monad.support.MonadSupportConstants
import monad.support.services.{MonadException, ServiceUtils}
import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.MergePolicy.MergeSpecification
import org.apache.lucene.index._
import org.apache.lucene.search.NumericRangeQuery
import org.apache.lucene.store.{Directory, IOContext, MMapDirectory, RateLimitedDirectoryWrapper}
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.slf4j.LoggerFactory

/**
 * 实现数据索引功能
 * @author jcai
 */
class ResourceIndexerImpl(rd: ResourceDefinition,
                          indexConfigSupport: IndexConfigSupport,
                          searcherSource: ResourceSearcherSource,
                          indexerManager: ResourceIndexerManager,
                          version: Int,
                          searchExecutor: ExecutorService)
  extends ResourceIndexer {
  private final val parser = new JsonParser
  private val logger = LoggerFactory getLogger getClass
  private val needMerge = new AtomicInteger()
  private val logFile = new File(indexConfigSupport.index.path + "/" + rd.name + "/_log_seq")
  private var analyzerObj: Analyzer = _
  private var indexWriter: IndexWriter = _
  private var mergeScheduler: ConcurrentMergeScheduler = _
  private var resourceSearcher: ResourceSearcher = _
  private var indexPath: File = _
  private var nosql: SlaveNo = _
  private var jobRunning = true

  def start() {
    logger.info("[{}] start node,version:{}", rd.name, version)
    //启动nosql
    val nosqlOptions = new NoSQLOptions()
    nosqlOptions.setCache_size_mb(indexConfigSupport.index.noSql.cache)
    nosqlOptions.setWrite_buffer_mb(indexConfigSupport.index.noSql.writeBuffer)
    nosqlOptions.setMax_open_files(indexConfigSupport.index.noSql.maxOpenFiles)
    nosqlOptions.setLog_keeped_num(indexConfigSupport.index.binlogLength)
    try {
      nosql = new NodeNoSQL(
        indexConfigSupport.index.noSql.path + "/" + rd.name,
        nosqlOptions)
      if (idNosql.isDefined)
        nosql.SetIdNoSQL(idNosql.get)
    } finally {
      nosqlOptions.delete()
    }
    init()
  }

  private[internal] def init() {
    if (this.analyzerObj == null) {
      this.analyzerObj = AnalyzerCreator.create(rd.index.analyzer)
    }

    val iwc = new IndexWriterConfig(MonadFaceConstants.LUCENE_VERSION,
      this.analyzerObj).
      //.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE).
      setRAMBufferSizeMB(indexConfigSupport.index.writerBuffer)


    mergeScheduler = new ConcurrentMergeScheduler {

      override def merge(writer: IndexWriter, trigger: MergeTrigger, newMergesFound: Boolean): Unit = {
        super.merge(writer, trigger, newMergesFound)
        super.sync()
        if (needMerge.get() > 0) {
          val seq = needMerge.decrementAndGet()
          logger.info("[{}] finish to merge,remain {} ....", rd.name, seq)
        }
      }
    }
    mergeScheduler.setMaxMergesAndThreads(6, 4)
    //mergeScheduler.setMaxThreadCount(4)
    //mergeScheduler.setMaxMergeCount(6)
    mergeScheduler.setMergeThreadPriority(Thread.MIN_PRIORITY + 1)
    iwc.setMergeScheduler(mergeScheduler)

    /*
    val tmp = new LogByteSizeMergePolicy
    tmp.setMergeFactor(10)
    //设置最大进行合并的索引
    tmp.setMaxMergeMB(7000)
    tmp.setUseCompoundFile(false)
    //设置非强制需要合并的索引,达到这个数值以后，将不能合并
    tmp.setMaxMergeMBForForcedMerge(10000)
    iwc.setMergePolicy(tmp)
    */
    val mergePolicy = new TieredMergePolicy() {

      override def findMerges(mergeTrigger: MergeTrigger, infos: SegmentInfos, writer: IndexWriter): MergeSpecification = {
        val mergeSpec = super.findMerges(mergeTrigger, infos, writer)

        if (mergeSpec != null && mergeSpec.merges.size() > 0) {
          val seq = needMerge.incrementAndGet()
          logger.info("[{}] begin to merge {} ....", rd.name, seq)
        }
        mergeSpec
      }
    }

    //mergePolicy.setUseCompoundFile(false)
    mergePolicy.setSegmentsPerTier(10)
    mergePolicy.setMaxMergeAtOnce(10)
    mergePolicy.setMaxMergeAtOnceExplicit(10)
    mergePolicy.setMaxMergedSegmentMB(18000)

    mergePolicy.setFloorSegmentMB(5)

    iwc.setMergePolicy(mergePolicy)

    //iwc.setReaderTermsIndexDivisor(4)

    indexPath = new File(indexConfigSupport.index.path + "/" + rd.name)
    logger.debug("[{}] index path is:{}", rd.name, indexPath)
    var directory: Directory = null
    indexerManager.getRateLimiter match {
      case Some(limiter) =>
        directory = new MMapDirectory(indexPath)
        val tmpDirectory = new RateLimitedDirectoryWrapper(directory)
        tmpDirectory.setRateLimiter(limiter, IOContext.Context.FLUSH)
        directory = tmpDirectory
      case None =>
        directory = new MMapDirectory(indexPath)
    }
    indexWriter = new IndexWriter(directory, iwc)
    try {
      val serverIdSupport = indexConfigSupport.asInstanceOf[ServerIdSupport]
      //如果大于1，则开启多线程支持，默认是单线程查询模式
      if (indexConfigSupport.index.queryThread > 1) {
        resourceSearcher = searcherSource.newResourceSearcher(rd, indexWriter, serverIdSupport.regionId, searchExecutor)
      }
      else
        resourceSearcher = searcherSource.newResourceSearcher(rd, indexWriter, serverIdSupport.regionId, null)
      resourceSearcher.asInstanceOf[ResourceSearcherImpl].config = indexConfigSupport
      resourceSearcher.start()
      maybeOutputRegionInfo(0)
    } catch {
      case e: Throwable =>
        shutdown()
        throw e
    }
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    logger.info("[{}] shutdown node", rd.name)
    jobRunning = false

    //关闭之前，先回滚已经提交的数据
    rollback()
    if (nosql != null) {
      nosql.StopNoSQL()
      nosql.delete()
    }

    if (resourceSearcher != null)
      resourceSearcher.shutdown()
    InternalUtils.close(indexWriter)
  }

  private def rollback() {
    if (indexWriter != null)
      indexWriter.rollback()
  }

  private def maybeOutputRegionInfo(lastSeq: Long) {
    //导到需要更新分区信息，或者不是数字整批提交模式
    if ((lastSeq & MonadFaceConstants.NUM_OF_NEED_UPDATE_REGION_INFO) == 0 ||
      (lastSeq & MonadFaceConstants.NUM_OF_NEED_COMMIT) != 0
    ) {
      val jsonObject = new JsonObject
      jsonObject.addProperty("data_count", nosql.GetDataStatCount())
      jsonObject.addProperty("binlog_seq", nosql.GetLastLogSeq())
      jsonObject.addProperty("index_count", indexWriter.maxDoc())
      indexerManager.setRegionInfo(rd.name, jsonObject)
    }
  }

  def getResourceSearcher = ServiceUtils.waitUntilObjectLive("%s搜索对象".format(rd.name)) {
    resourceSearcher
  }

  def removeIndex() {
    logger.info("[{}] remove index ....", rd.name)
    var tmpPath = new File(indexConfigSupport.index.path + "/" + rd.name + ".tmp")
    FileUtils.moveDirectory(indexPath, tmpPath)
    FileUtils.deleteQuietly(tmpPath)
    //删除NoSQL数据
    val originPath = new File(indexConfigSupport.index.noSql.path + "/" + rd.name)
    tmpPath = new File(indexConfigSupport.index.noSql.path + "/" + rd.name + ".tmp")
    FileUtils.moveDirectory(originPath, tmpPath)
    FileUtils.deleteQuietly(tmpPath)
  }

  def DoSyncData() {
    logger.info("[{}] start to sync data ,start log {}", rd.name, nosql.GetLastLogSeq())
    nosql.DoSyncData(indexConfigSupport.index.syncServer, rd.name, indexConfigSupport.regionId)
    logger.info("[{}] finish to sync data,last log seq {} data:" + nosql.GetDataStatCount(), rd.name, nosql.GetLastLogSeq())
  }

  def index() {
    val indexSeq = readLastLog() + 1
    val nosqlSeq = nosql.GetLastLogSeq()
    var log_data: Array[Byte] = null
    var seq_running = 0L
    indexSeq to nosqlSeq foreach { seq =>
      if (!jobRunning)
        throw new MonadException(MonadNodeExceptionCode.INDEXER_WILL_SHUTDOWN)
      log_data = nosql.FindNextBinlog(seq)
      if (log_data == null) {
        throw new MonadException(MonadNodeExceptionCode.NOSQL_LOG_DATA_IS_NULL)
      }
      val binlog = new NoSQLBinlogValue(log_data)
      val keyBytes = binlog.keyBytes()
      val valBytes = nosql.Get(keyBytes)
      var data: JsonObject = null
      if (valBytes != null) {
        data = parser.parse(new String(valBytes, MonadSupportConstants.UTF8_ENCODING)).getAsJsonObject
      }
      val command = binlog.commandType()
      command match {
        case DataCommandType.PUT | DataCommandType.UPDATE =>
          if (data != null) {
            indexData(DataTypeUtils.convertAsInt(keyBytes), data, command.swigValue(), binlog.objectId())
          }
        case DataCommandType.DEL =>
          indexData(DataTypeUtils.convertAsInt(keyBytes), data, command.swigValue(), None)

      }
      seq_running = seq

      //分步提交
      if ((seq_running & MonadFaceConstants.NUM_OF_NEED_COMMIT) == 0) {
        triggerCommit(seq_running)
      }
    }
    if (seq_running > 0) {
      triggerCommit(seq_running)
      //删除本NoSQL的binlog日志,保留最新的10条
      if (seq_running > 10)
        nosql.DeleteBinlog(seq_running - 10)
    }
    else
      logger.info("[{}] no data indexed,current log_seq {}", rd.name, nosqlSeq)
  }

  def indexData(id: Int, row: JsonObject, command: Int, objectId: Option[Int]) {
    indexerManager.getDisruptor.publishEvent(new EventTranslator[IndexEvent] {
      def translateTo(event: IndexEvent, sequence: Long) {
        event.reset()
        event.resource = rd
        event.id = id
        event.row = row
        event.version = version
        event.command = command
        event.objectId = objectId
      }
    })
  }

  def triggerCommit(logSeq: Long) {
    logger.debug("commit log seq :{}", logSeq)
    indexerManager.getDisruptor.publishEvent(new EventTranslator[IndexEvent] {
      def translateTo(event: IndexEvent, sequence: Long) {
        event.reset()
        event.resource = rd
        event.commitFlag = true
        event.commitSeq = logSeq
        event.version = version
      }
    })
  }

  private def readLastLog(): Long = {
    if (!logFile.exists()) return 0L
    val bytes = FileUtils.readFileToByteArray(logFile)
    DataTypeUtils.convertAsLong(Some(bytes)).get
  }

  def indexDocument(doc: Document, dataVersion: Int) {
    obtainIndexWriter
    if (isSameVersion(dataVersion))
      indexWriter.addDocument(doc)
  }

  private def isSameVersion(dataVersion: Int): Boolean = {
    if (version != dataVersion) {
      logger.warn("[" + rd.name + "] indexer version({}) != data version({})", version, dataVersion)
      return false
    }
    true
  }

  private def obtainIndexWriter: IndexWriter = ServiceUtils.waitUntilObjectLive("%s索引对象".format(rd.name)) {
    indexWriter
  }

  def updateDocument(id: Int, doc: Document, dataVersion: Int) {
    obtainIndexWriter
    if (isSameVersion(dataVersion)) {
      val query = NumericRangeQuery.newIntRange(MonadFaceConstants.OBJECT_ID_FIELD_NAME, id, id, true, true)
      //indexWriter.deleteDocuments(new Term(MonadFaceConstants.OBJECT_ID_FIELD_NAME,NumericUtils.intToPrefixCoded(id)))
      indexWriter.deleteDocuments(query)
      indexWriter.addDocument(doc)
    }
  }

  def deleteDocument(id: Int, dataVersion: Int) {
    obtainIndexWriter
    if (isSameVersion(dataVersion)) {
      val query = NumericRangeQuery.newIntRange(MonadFaceConstants.OBJECT_ID_FIELD_NAME, id, id, true, true)
      indexWriter.deleteDocuments(query)
      //indexWriter.deleteDocuments(new Term(MonadFaceConstants.OBJECT_ID_FIELD_NAME,NumericUtils.intToPrefixCoded(id)))
    }
  }

  def commit(lastSeq: Long, dataVersion: Int) {
    obtainIndexWriter

    indexWriter.commit()
    writeLastLog(lastSeq)
    maybeOutputRegionInfo(lastSeq)

    resourceSearcher.maybeRefresh()
    val i = indexWriter.maxDoc()
    logger.info("[{}] {} records commited,log seq :" + lastSeq, rd.name, i)
  }

  private def writeLastLog(seq: Long) {
    FileUtils.writeByteArrayToFile(logFile, DataTypeUtils.convertAsArray(seq))
  }

  def findObject(key: Array[Byte]) = {
    val value = nosql.Get(key)
    if (value != null) Some(value) else None
  }

  override def toString = {
    "%s indexer".format(rd.name)
  }

  def findObjectId(idSeq: Int) = {
    val buffer = ByteBuffer.allocate(5)
    buffer.putChar(DataType.ID_SEQ.swigValue().asInstanceOf[Char])
    buffer.putInt(idSeq)
    val value = nosql.RawGet(buffer.array())
    if (value == null) None else Some(value)
  }
}
