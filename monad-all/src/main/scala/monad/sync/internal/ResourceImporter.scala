// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.internal

import java.sql.{Connection, ResultSet, SQLException}
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Future
import java.util.concurrent.locks.LockSupport
import java.util.regex.Pattern
import monad.core.services.{StartAtDelay, StartAtOnce, CronScheduleWithStartModel}
import monad.face.config.SyncConfigSupport
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.model._
import monad.face.model.types.{DateColumnType, IntColumnType, LongColumnType, StringColumnType}
import monad.support.services.{MonadException, ServiceLifecycle, ServiceUtils}
import monad.sync.internal.JdbcDatabase._
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.ioc.services.ParallelExecutor
import org.apache.tapestry5.ioc.services.cron.{PeriodicExecutor, PeriodicJob}
import org.slf4j.LoggerFactory

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
                       val saver: ResourceSaver,
                       syncConfig: SyncConfigSupport)
  extends ServiceLifecycle {
  private val logger = LoggerFactory getLogger getClass
  private val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  //所有列
  private val columns = rd.properties
  private val jobName = "monad-importer-%s".format(rd.name)
  private var isFull = false
  private var modifyKeyColumn: ResourceProperty = null
  private var modifyKeyColumnIndex: Int = -1
  //增量的SQL
  private var incrementSql: String = _
  //最小值的sql
  private var minValueSql: String = _
  //最大值的sql
  private var maxValueSql: String = _
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

  def start() {
    logger.info("[{}] start importer with version:{}", rd.name, version)
    init()
    startScheduler()
  }

  def init() {
    isFull = rd.sync.policy == SyncPolicy.Full
    //检查增量字段列定义
    checkModifyKey()

    //验证资源配置
    validateResourceConfig()

    //检查列
    modifyDefaultColumnTypeAsString()

    //构造SQL
    buildSQL()

  }

  private def buildSQL() {
    //创建select语句
    val selects = columns.map(_.name).map { name =>
      if (name.startsWith("_")) "\"" + name + "\"" else name
    }.mkString(",")

    isFull match {
      case true =>
        initFullSyncSQLByDriver("select " + selects + " from (" + rd.sync.jdbc.sql + ")")
      case false =>
        val sqlBuilder = new StringBuilder()
        sqlBuilder.append("select " + selects + " from (").append(rd.sync.jdbc.sql).append(" ) x_  where ")
        sqlBuilder.append(modifyKeyColumn.name).append(">?").append(" and ").
          append(modifyKeyColumn.name).append("<=?").
          append(" order by ").append(modifyKeyColumn.name).append(" asc")

        //增量数据的Sql
        incrementSql = sqlBuilder.toString()
        //最小时间sql
        minValueSql = "select min(" + modifyKeyColumn.name + ") from ( " + rd.sync.jdbc.sql + " ) x_"
        maxValueSql = "select max(" + modifyKeyColumn.name + ") from ( " + rd.sync.jdbc.sql + " ) x_"
    }

  }

  private def initFullSyncSQLByDriver(sql: String) {
    if (rd.sync.jdbc.driver.indexOf("oracle") > -1) {
      incrementSql = oracleGetLimitString(sql)
      minValueSql = oracleGetLimitString(sql, Some("min"))
      maxValueSql = oracleGetLimitString(sql, Some("max"))
    } else if (rd.sync.jdbc.driver.indexOf("ibm") > -1) {
      incrementSql = db2GetLimitString(sql)
      minValueSql = db2GetLimitString(sql, Some("min"))
      maxValueSql = db2GetLimitString(sql, Some("max"))
    } else if (rd.sync.jdbc.driver.indexOf("mysql") > -1) {
      incrementSql = mysqlGetLimitString(sql)
      minValueSql = mysqlGetLimitString(sql, Some("min"))
      maxValueSql = mysqlGetLimitString(sql, Some("max"))
    }
  }

  //=== mysql
  private def mysqlGetLimitString(sqlStr: String, valueQuery: Option[String] = None): String = {
    valueQuery match {
      case Some(str) =>
        "select " + str + " from (" + sqlStr + ") x_ limit ?,?"
      case None =>
        sqlStr + " limit ?, ?"
    }
  }

  //=== oracle implements from org.hibernate.dialect.Oracle8iDialect
  private def oracleGetLimitString(sqlStr: String, valueQuery: Option[String] = None): String = {
    val sql = sqlStr.trim()

    val pagingSelect = new StringBuffer(sql.length() + 100)

    valueQuery match {
      case Some(str) =>
        pagingSelect.append("select ").append(str).append("(rownum) from ( ")
      case None =>
        pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ")
    }

    pagingSelect.append(sql)

    valueQuery match {
      case Some(str) =>
        pagingSelect.append(" ) ")
      case None =>
        pagingSelect.append(" ) row_ ) where rownum_ > ? and rownum_ <= ?")
    }

    pagingSelect.toString
  }

  private def db2GetLimitString(sql: String, valueQuery: Option[String] = None): String = {
    val startOfSelect = sql.toLowerCase.indexOf("select")

    val pagingSelect = new StringBuffer(sql.length() + 100)
      .append(sql.substring(0, startOfSelect)) // add the comment
      .append("select ")
    if (valueQuery.isDefined)
      pagingSelect.append(valueQuery.get).append("(rownumber_)")
    else
      pagingSelect.append("*")
    pagingSelect.append(" from ( select ") // nest the main query in an outer select
      .append(db2GetRowNumber(sql)); // add the rownnumber bit into the outer query select list

    if (db2HasDistinct(sql)) {
      pagingSelect.append(" row_.* from ( ") // add another (inner) nested select
        .append(sql.substring(startOfSelect)) // add the main query
        .append(" ) as row_"); // close off the inner nested select
    } else {
      pagingSelect.append(sql.substring(startOfSelect + 6)); // add the main query
    }

    pagingSelect.append(" ) as temp_")

    //add the restriction to the outer select
    if (valueQuery.isEmpty)
      pagingSelect.append(" where rownumber_ between ?+1 and ?")

    pagingSelect.toString
  }

  private def db2GetRowNumber(sql: String): String = {
    val rownumber = new StringBuffer(50)
      .append("rownumber() over(")

    val orderByIndex = sql.toLowerCase.indexOf("order by")

    if (orderByIndex > 0 && !db2HasDistinct(sql)) {
      rownumber.append(sql.substring(orderByIndex))
    }

    rownumber.append(") as rownumber_,")

    rownumber.toString
  }

  //=== db2 implements from org.hibernate.dialect.DB2Dialect
  private def db2HasDistinct(sql: String): Boolean = {
    sql.toLowerCase.indexOf("select distinct") >= 0
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

  private def checkModifyKey() {
    if (isFull) {
      modifyKeyColumn = new ResourceProperty
      modifyKeyColumn.columnType = ColumnType.Long
      return
    }

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
        logger.error("[" + rd.name + "] fail to import data ,sql:\n" + incrementSql, e)
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
      logger.info("[{}]sql:{}", rd.name, incrementSql)
    }
    //先得到本身库中的最大值
    var maxValue = saver.findMaxValue
    if (maxValue.isEmpty) {
      //查询库中的最小值当做开始值
      maxValue = queryOne[Option[Long]](minValueSql) { rs =>
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
    dbMaxValue = queryOne(maxValueSql) { rs =>
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
      val st = conn.prepareStatement(incrementSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
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
          case e =>
            //假如设置忽略
            if (syncConfig.sync.ignore_data_when_unqualified_field) {
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

  }

  override def toString = {
    "%s importer".format(rd.name)
  }
}
