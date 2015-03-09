// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

import com.google.gson.{JsonObject, JsonParser}
import com.lmax.disruptor.EventTranslator
import monad.face.MonadFaceConstants
import monad.face.config.IndexConfigSupport
import monad.face.model.{AnalyzerCreator, IndexEvent, ResourceDefinition}
import monad.face.services.{DataTypeUtils, ResourceSearcher, ResourceSearcherSource}
import monad.jni.services.gen.{DataCommandType, NormalSeqDataKey, SlaveNoSQLSupport}
import monad.node.services.{MonadNodeExceptionCode, ResourceIndexer, ResourceIndexerManager}
import monad.support.MonadSupportConstants
import monad.support.services.{LoggerSupport, MonadException, ServiceUtils}
import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.MergePolicy.MergeSpecification
import org.apache.lucene.index._
import org.apache.lucene.store._
import org.apache.lucene.util.{BytesRefBuilder, NumericUtils}
import org.apache.tapestry5.ioc.internal.util.InternalUtils

import scala.annotation.tailrec

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
  extends NodeNoSQLServiceImpl(indexConfigSupport)
  with ResourceIndexer with LoggerSupport {
  private final val parser = new JsonParser
  private val needMerge = new AtomicInteger()
  private val logFile = new File(indexConfigSupport.index.path + "/" + rd.name + "/_log_seq")
  private var analyzerObj: Analyzer = _
  private var indexWriter: IndexWriter = _
  private var mergeScheduler: ConcurrentMergeScheduler = _
  private var resourceSearcher: ResourceSearcher = _
  private var indexPath: File = _
  private var jobRunning = true

  def start() {
    logger.info("[{}] start node,version:{}", rd.name, version)
    startNoSQLInstance(rd)
    init()
  }

  private[internal] def init() {
    if (this.analyzerObj == null) {
      this.analyzerObj = AnalyzerCreator.create(rd.index.analyzer)
    }

    val iwc = new IndexWriterConfig(this.analyzerObj).
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
    //mergeScheduler.setMergeThreadPriority(Thread.MIN_PRIORITY + 1)
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
    var directory: Directory = FSDirectory.open(indexPath.toPath)
    indexerManager.getRateLimiter foreach { limiter =>
      //TODO 增加限速支持
      directory = new FilterDirectory(directory) {
        override def createOutput(name: String, context: IOContext): IndexOutput = {
          val output = super.createOutput(name, context)
          new RateLimitedIndexOutput(limiter, output)
        }
      }
    }
    indexWriter = new IndexWriter(directory, iwc)
    try {
      //如果大于1，则开启多线程支持，默认是单线程查询模式
      if (indexConfigSupport.index.queryThread > 1) {
        resourceSearcher = searcherSource.newResourceSearcher(rd, indexWriter, indexConfigSupport.partitionId, searchExecutor)
      }
      else
        resourceSearcher = searcherSource.newResourceSearcher(rd, indexWriter, indexConfigSupport.partitionId, null)
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
    shutdownNoSQLInstance()

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
    /*
    if ((lastSeq & MonadFaceConstants.NUM_OF_NEED_UPDATE_REGION_INFO) == 0 ||
      (lastSeq & MonadFaceConstants.NUM_OF_NEED_COMMIT) != 0
    ) {
      val jsonObject = new JsonObject
      jsonObject.addProperty("data_count", nosql.GetDataStatCount())
      jsonObject.addProperty("binlog_seq", nosql.GetLastLogSeq())
      jsonObject.addProperty("index_count", indexWriter.maxDoc())
      indexerManager.setRegionInfo(rd.name, jsonObject)
    }
    */
  }

  def getResourceSearcher = ServiceUtils.waitUntilObjectLive("%s搜索对象".format(rd.name)) {
    resourceSearcher
  }

  def removeIndex() {
    logger.info("[{}] remove index ....", rd.name)
    val tmpPath = new File(indexConfigSupport.index.path + "/" + rd.name + "." + System.currentTimeMillis())
    FileUtils.moveDirectory(indexPath, tmpPath)
    FileUtils.deleteQuietly(tmpPath)
    //删除NoSQL数据
    destryNoSQL(rd)
  }

  def index(): Unit = {
    val nosql = nosqlOpt().get
    val latestSeq = nosql.FindLastBinlog()
    val fromSeq = readLastLog() + 1
    if (fromSeq <= latestSeq) {
      info("begin to index from {} to {}", fromSeq, latestSeq)
      val range = Range.Long.inclusive(fromSeq, latestSeq, 1)
      indexRange(-1, nosql, range)
      info("finish to index from {} to {}", fromSeq, latestSeq)
    }
    if (latestSeq - fromSeq > 100) {
      nosql.DeleteBinlogRange(fromSeq, latestSeq - 100)
    }
    else
      logger.info("[{}] no data indexed,current log_seq {}", rd.name, latestSeq)
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

  def indexDocument(doc: Document, dataVersion: Int) {
    obtainIndexWriter
    if (isSameVersion(dataVersion))
      indexWriter.addDocument(doc)
  }

  def updateDocument(id: Int, doc: Document, dataVersion: Int) {
    obtainIndexWriter
    if (isSameVersion(dataVersion)) {
      indexWriter.updateDocument(createIdTerm(id), doc)
    }
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

  private def createIdTerm(id: Int) = {
    val bb = new BytesRefBuilder
    NumericUtils.intToPrefixCoded(id, 0, bb)
    new Term(MonadFaceConstants.OBJECT_ID_FIELD_NAME, bb.get)
  }

  def deleteDocument(id: Int, dataVersion: Int) {
    obtainIndexWriter
    if (isSameVersion(dataVersion)) {
      indexWriter.deleteDocuments(createIdTerm(id))
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
    val nosqlKey = new NormalSeqDataKey(indexConfigSupport.partitionId, DataTypeUtils.convertAsInt(key));
    val value = nosqlOpt().get.Get(nosqlKey)
    if (value != null) Some(value) else None
  }

  override def toString = {
    "%s indexer".format(rd.name)
  }

  def findObjectId(idSeq: Int) = {
    /*
    val buffer = ByteBuffer.allocate(5)
    buffer.putChar(DataType.ID_SEQ.swigValue().asInstanceOf[Char])
    buffer.putInt(idSeq)
    val value = nosql.RawGet(buffer.array())
    if (value == null) None else Some(value)
  */
    None
  }

  @tailrec
  private def indexRange(lastSeq: Long, nosql: SlaveNoSQLSupport, range: Seq[Long]): Unit = {
    range.headOption match {
      case Some(seq) =>
        //commit
        if (lastSeq % indexConfigSupport.index.needCommit == 0) {
          triggerCommit(lastSeq)
        }
        val binlogValue = nosql.FindNextBinlog(seq)
        if (binlogValue == null) {
          throw new MonadException(MonadNodeExceptionCode.NOSQL_LOG_DATA_IS_NULL)
        }
        val nosqlKey = new NormalSeqDataKey(binlogValue.Key())
        val key = nosqlKey.DataSeq()
        val valBytes = nosql.Get(nosqlKey)

        var data: JsonObject = null
        if (valBytes != null) {
          data = parser.parse(new String(valBytes, MonadSupportConstants.UTF8_ENCODING)).getAsJsonObject
        }
        val command = binlogValue.CommandType()
        command match {
          case DataCommandType.PUT | DataCommandType.UPDATE =>
            if (data != null) {
              indexData(key, data, command.swigValue(), None)
            }
          case DataCommandType.DEL =>
            indexData(key, data, command.swigValue(), None)

        }

        indexRange(seq, nosql, range.tail)
      case None =>
        //commit
        triggerCommit(lastSeq)
    }
  }

  private def readLastLog(): Long = {
    if (!logFile.exists()) return 0L
    val bytes = FileUtils.readFileToByteArray(logFile)
    DataTypeUtils.convertAsLong(Some(bytes)).get
  }
}
