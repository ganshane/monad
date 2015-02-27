// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import java.sql.{Connection, ResultSet, SQLException}
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Future
import java.util.concurrent.locks.LockSupport
import java.util.regex.Pattern

import monad.core.services.{CronScheduleWithStartModel, StartAtDelay, StartAtOnce}
import monad.face.config.SyncConfigSupport
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.model._
import monad.face.model.types.{DateColumnType, IntColumnType, LongColumnType, StringColumnType}
import monad.support.services.{LoggerSupport, MonadException, ServiceLifecycle, ServiceUtils}
import monad.sync.internal.JdbcDatabase._
import monad.sync.services.ResourceImporterManager
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.ioc.services.ParallelExecutor
import org.apache.tapestry5.ioc.services.cron.{PeriodicExecutor, PeriodicJob}

import scala.collection.JavaConversions._

/**
 * 针对资源的导入
 * @author jcai
 */
class ResourceImporter(val rd: ResourceDefinition,
                       importerManager: ResourceImporterManager,
                       periodicExecutor: PeriodicExecutor,
                       parallelExecutor: ParallelExecutor,
                       version: Int,
                       syncConfig: SyncConfigSupport)
  extends ServiceLifecycle
  with SyncNoSQLSupport
  with LoggerSupport
  with ResourceConfigLike {
  private val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  //所有列
  private val columns = rd.properties
  private val jobName = "monad-importer-%s".format(rd.name)
  private val dataFetcher: DataFetcher = DataFetcher(this)
  private var isFull = false
  private var modifyKeyColumn: ResourceProperty = null
  private var modifyKeyColumnIndex: Int = -1
  //定时器
  private var job: PeriodicJob = _
  //导入的线程
  private var importerFuture: Future[_] = null
  private[internal] implicit var conn: Connection = null;
  @volatile
  private var running: Boolean = true
  @volatile
  private var stopped: Boolean = true
  @volatile
  private var importerThread: Thread = null

  /**
   * 增量字段定义
   * @return 增量字段定义
   */
  override def incrementColumn: ResourceProperty = findIncrementColumn()

  /**
   * 得到资源配置定义
   * @return 资源配置定义
   */
  override def resourceDefinition: ResourceDefinition = rd

  def start() {
    logger.info("[{}] start importer with version:{}", rd.name, version)
    init()
    startScheduler()
    startNoSQL()
  }

  def init() {
    isFull = rd.sync.policy == SyncPolicy.Full
    //检查增量字段列定义
    findIncrementColumn()

    //验证资源配置
    validateResourceConfig()

    //检查列
    modifyDefaultColumnTypeAsString()

  }

  private def findIncrementColumn(): ResourceProperty = {
    val isFull = rd.sync.policy == SyncPolicy.Full
    if (isFull) {
      modifyKeyColumn = new ResourceProperty
      modifyKeyColumn.columnType = ColumnType.Long
    } else {
      for ((col, index) <- columns.view.zipWithIndex if col.modifyKey) {
        if (modifyKeyColumn != null) {
          throw new MonadException("重复定义增量列字段", MonadSyncExceptionCode.DUPLICATE_INCREMENT_COLUMN)
        }
        modifyKeyColumn = col
        modifyKeyColumnIndex = index
      }
      if (modifyKeyColumn == null)
        throw new MonadException("未定义增量数据判断列", MonadSyncExceptionCode.INCREMENT_COLUMN_NOT_DEFINED)
    }
    modifyKeyColumn
  }

  private def modifyDefaultColumnTypeAsString() {
    for ((col, index) <- columns.view.zipWithIndex if col.columnType == null) {
      col.columnType = ColumnType.String
    }
    //设置列和资源定义相互引用
    for ((col, index) <- columns.view.zipWithIndex) {
      col.resourceDefinition = rd
    }
  }

  private def validateResourceConfig() {
    if (rd.resourceType == ResourceType.Data) {
      if (InternalUtils.isBlank(rd.targetResource)) {
        throw new MonadException("resource is data,so target attribute need configuration", MonadSyncExceptionCode.TARGET_RESOURCE_NOT_EXIST)
      }
    }
  }

  private[internal] def startScheduler() {

    if (rd.name.startsWith("test")) {
      job = periodicExecutor.addJob(new CronScheduleWithStartModel(rd.sync.cron, StartAtOnce), jobName, new Runnable {
        def run() {
          var num = 10
          val pattern = Pattern.compile("test_([0-9]+)")
          val matcher = pattern.matcher(rd.name)
          if (matcher.matches()) {
            num = matcher.group(1).toInt
          }
          importerThread = Thread.currentThread()
          0 until num foreach { i =>
            val result = new Array[Any](columns.length)
            var j = 0
            columns.foreach { cd =>
              cd.columnType.getColumnType match {
                case c: StringColumnType =>
                  result(j) = rd.name + i
                case c: IntColumnType =>
                  result(j) = 123 + i
                case c: DateColumnType =>
                  result(j) = 1234L + i
                case c: LongColumnType =>
                  result(j) = 4321L + i
              }
              j += 1
            }
            importerManager.importData(rd.name, result, System.currentTimeMillis(), version)
          }
          info("[{}] finish to import test data", rd.name)
        }
      })
    } else {
      isFull match {
        case true =>
          importerFuture = importerManager.submitSync {
            doImport()
            job = periodicExecutor.addJob(new CronScheduleWithStartModel(rd.sync.cron, StartAtDelay), jobName, new Runnable {
              def run() {
                importerManager.resync(rd.name)
              }
            })
          }
        case false =>
          job = periodicExecutor.addJob(new CronScheduleWithStartModel(rd.sync.cron, StartAtOnce), jobName, new Runnable {
            def run() {
              importerFuture = importerManager.submitSync {
                doImport()
              }
            }
          })
      }
    }
  }

  def doImport() {
    if (!stopped) {
      return
    }
    stopped = false

    val threadName = Thread.currentThread().getName
    try {
      Thread.currentThread().setName(jobName)
      buildConnection
      importData()
    } catch {
      case e: Throwable =>
        logger.error("[" + rd.name + "] fail to import data ,sql:\n" + dataFetcher.buildIncrementSQL(), e)
    }

    closeJdbc(conn)
    stopped = true
    //构造停止标记
    importerFuture = null
    if (threadName != null) {
      Thread.currentThread().setName(threadName)
    }
  }

  def importData() {
    logger.info("[{}] load data ...", rd.name)
    if (System.getProperty("showsql", "false") == "true") {
      logger.info("[{}]sql:{}", rd.name, dataFetcher.buildIncrementSQL())
    }
    //先得到本身库中的最大值
    var maxValue = findMaxTimestamp()
    if (maxValue.isEmpty) {
      //查询库中的最小值当做开始值
      maxValue = queryOne[Option[Long]](dataFetcher.buildMinValueSQL()) { rs =>
        val maxValueOption = modifyKeyColumn.readJdbcValue(rs, 1).asInstanceOf[Option[Long]]
        if (maxValueOption.isDefined)
          Some(maxValueOption.get - 1000) //向前一秒
        else None
      }
    }
    if (maxValue.isEmpty) {
      logger.warn("[{}] data start value not found,no data?", rd.name)
      importerManager.importData(rd.name, null, 0, version)
      return
    }
    importerManager.importData(rd.name, null, maxValue.get, version)

    //得到目标库中的最大值
    var dbMaxValue: Option[Long] = None
    dbMaxValue = queryOne(dataFetcher.buildMaxValueSQL()) { rs =>
      modifyKeyColumn.readJdbcValue(rs, 1).asInstanceOf[Option[Long]]
    }
    if (dbMaxValue.isEmpty) {
      logger.info("[{}] database max value not found", rd.name)
      return
    }

    if (dbMaxValue.get > maxValue.get) {
      var beginValue = maxValue.get
      var endValue = beginValue

      logger.info("[{}] begin import from {}({}) to {}({}) ", Array[Object](
        rd.name,
        formatter.format(new Date(beginValue)),
        beginValue.asInstanceOf[Object],
        formatter.format(new Date(dbMaxValue.get)),
        dbMaxValue.get.asInstanceOf[Object]))

      var totalNum: Int = 0
      do {
        beginValue = endValue
        endValue = endValue + (rd.sync.interval * 60 * 1000).toLong
        //如果累计的结果操作数据库的最大值，则使用数据库最大值
        if (endValue > dbMaxValue.get) endValue = dbMaxValue.get
        totalNum = doSync(beginValue, endValue, totalNum)
        //将不在设置最大值，由trigger commit统一设置
        //importerManager.setMaxValue(rd,endValue)
      } while (running && dbMaxValue.get > endValue)
      //等待1分钟，其他数据完成操作
      importerManager.importData(rd.name, null, endValue, version)
      logger.info("[{}] total {} data imported", rd.name, totalNum)
    } else {
      logger.info("[{}] no newer record found,dbMax:{} sysMax:{}", Array[Object](rd.name, dbMaxValue, maxValue))
    }
  }

  def doSync(beginValue: Long, endValue: Long, totalNum: Int): Int = {
    logger.debug("[{}]begin {} to {}", Array[Object](
      rd.name,
      formatter.format(new Date(beginValue)),
      formatter.format(new Date(endValue))))

    var num = totalNum
    use(autoCommit = false) { conn =>
      val st = conn.prepareStatement(dataFetcher.buildIncrementSQL(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      st.setFetchSize(rd.sync.jdbc.batchSize)
      try {
        modifyKeyColumn.columnType.getColumnType.asInstanceOf[MonadColumnType[Long]].setJdbcParameter(st, 1, beginValue, modifyKeyColumn)
        modifyKeyColumn.columnType.getColumnType.asInstanceOf[MonadColumnType[Long]].setJdbcParameter(st, 2, endValue, modifyKeyColumn)
        val rs = st.executeQuery
        try {
          while (running && rs.next) {
            if (processRow(rs, num)) {
              num += 1
            }
          }
        } finally {
          closeJdbc(rs)
        }
      } finally {
        closeJdbc(st)
      }
    }
    num
  }

  private def processRow(rs: ResultSet, num: Int): Boolean = {
    try {
      var timestamp = if (isFull) Some(0L) else None
      val data = new Array[Any](columns.length)

      for ((col, index) <- columns.view.zipWithIndex) {
        try {
          val value = col.readJdbcValue(rs, index + 1)
          //如果是增量抽取，则读取增量字段内容
          if (!isFull && index == modifyKeyColumnIndex)
            timestamp = value.asInstanceOf[Option[Long]]

          value match {
            case Some(v) =>
              data(index) = v
            case None =>
              data(index) = null
          }
        } catch {
          case se: SQLException =>
            val me = MonadException.wrap(se,
              MonadSyncExceptionCode.FAIL_READ_DATA_FROM_DB,
              "fail to read " + col.name + " value from db")
            throw me;
          case e: Throwable =>
            //假如设置忽略
            if (config.sync.ignore_data_when_unqualified_field) {
              throw e;
            } else {
              //不忽略当前行，则仅仅输出
              logger.warn("[{}] import row data,though fail to read " + col.name + " value from db,{}", rd.name, e.toString)
            }
        }
      }

      if (!isFull && timestamp.isEmpty) {
        //基本上不会出现
        logger.warn("[{}] 增量字段数据为空", rd.name)
        return false
      }
      //导入记录
      importerManager.importData(rd.name, data, timestamp.get, version)
      return true
    } catch {
      case e: MonadException =>
        if (rd.sync.showBadRecordException) {
          logger.error("[{}] {} ", rd.name, e.toString)
        }
      case e: Throwable =>
        if (rd.sync.showBadRecordException) {
          logger.warn("[" + rd.name + "] fail to import row", e)
        }
    }
    false
  }

  /**
   * 得到全局的同步配置定义
   * @return 同步配置定义
   */
  override def config: SyncConfigSupport = syncConfig

  private[internal] def buildConnection()() {
    if (conn == null || conn.isClosed) {
      logger.debug("[{}] get connection.....", rd.name)
      conn = JdbcDatabase.getConnection(rd.sync.jdbc.driver, rd.sync.jdbc.url, rd.sync.jdbc.user, rd.sync.jdbc.password)
      logger.debug("[{}] connected", rd.name)
    }
  }

  def shutdown() {

    logger.info("[{}] shutdown importer ", rd.name)
    running = false

    if (job != null)
      job.cancel()

    if (importerFuture != null) {
      //终止导入线程
      ServiceUtils.runInNoThrow({
        importerFuture.cancel(true)
      })
    }
    while (!stopped) {
      //等待导入线程结束
      LockSupport.parkNanos(1000)
    }
    if (conn != null) {
      closeJdbc(conn)
    }
    shutdownNoSQL()
  }

  override def toString = {
    "%s importer".format(rd.name)
  }

}
