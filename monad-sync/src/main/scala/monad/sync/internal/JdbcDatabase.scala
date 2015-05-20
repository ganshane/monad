// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import java.sql._
import javax.sql.DataSource

import monad.support.services.MonadException
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

/**
 * jdbc操作数据库的通用类
 */
private[internal] object JdbcDatabase {
  private val logger = LoggerFactory getLogger getClass

  def use[T](autoCommit: Boolean = true)(action: Connection => T)(implicit conn: Connection): T = {
    try {
      conn.setAutoCommit(autoCommit)
      val ret = action(conn)
      if (!autoCommit) conn.commit()
      ret
    } catch {
      case NonFatal(e) =>
        if (!autoCommit) conn.rollback()
        throw new MonadException(e, MonadSyncExceptionCode.JDBC_ERROR)
    }
  }

  def update(sql: String, params: Seq[Seq[_]] = Nil, timeoutSec: Int = 0)(implicit conn: Connection): Seq[Int] = {
    val st = conn.prepareStatement(sql)
    try {
      st.setQueryTimeout(timeoutSec)
      if (supportBatch(conn)) {
        if (params.isEmpty) st.addBatch()
        else for (p <- params) {
          setParams(st, p); st.addBatch()
        }
        st.executeBatch().toSeq
      } else {
        val updates = ListBuffer[Int]()
        if (params.isEmpty) updates += st.executeUpdate()
        for (p <- params) {
          setParams(st, p); updates += st.executeUpdate()
        }
        updates.toSeq
      }
    } finally {
      closeJdbc(st)
    }
  }

  private def supportBatch(conn: Connection) = conn.getMetaData.supportsBatchUpdates

  def execute(sql: String)(implicit conn: Connection) {
    val st = conn.prepareStatement(sql)
    try {
      st.execute()
    } finally {
      closeJdbc(st)
    }
  }

  def closeJdbc(resource: Any) {
    if (resource == null) return
    try {
      resource match {
        case c: Connection =>
          c.close()
        case s: Statement =>
          s.close()
        case r: ResultSet =>
          r.close()
        case _ => // do nothing
      }
    } catch {
      case NonFatal(e) => logger.error(e.getMessage, e)
    }
  }

  def queryOne[T](sql: String, params: Seq[_] = Nil, timeoutSec: Int = 0)(mapper: ResultSet => T)(implicit conn: Connection): T = {
    val st = conn.prepareStatement(sql)
    try {
      setParams(st, params)
      val rs = st.executeQuery
      try {
        if (rs.next)
          mapper(rs)
        else
          null.asInstanceOf[T]
      } finally {
        closeJdbc(rs)
      }
    } finally {
      closeJdbc(st)
    }
  }

  private def setParams(st: PreparedStatement, params: Seq[_]) {
    for (i <- 1 to params.size) {
      val value = params(i - 1)
      if (value.isInstanceOf[java.util.Date]) {
        st.setTimestamp(i, new Timestamp(params(i - 1).asInstanceOf[java.util.Date].getTime))
      } else {
        st.setObject(i, params(i - 1))
      }
    }
  }

  def query[T](sql: String, params: Seq[_] = Nil, timeoutSec: Int = 0)(mapper: ResultSet => T)(implicit conn: Connection) {
    val st = conn.prepareStatement(sql)
    try {
      setParams(st, params)
      val rs = st.executeQuery
      try {
        while (rs.next) mapper(rs)
      } finally {
        closeJdbc(rs)
      }
    } finally {
      closeJdbc(st)
    }
  }

  def queryWithPsSetter[T](sql: String, params: Seq[(PreparedStatement) => Unit] = Nil, timeoutSec: Int = 0)(mapper: ResultSet => T)(implicit conn: Connection) {
    val st = conn.prepareStatement(sql)
    try {
      params.foreach(f => f(st))
      val rs = st.executeQuery
      try {
        while (rs.next) mapper(rs)
      } finally {
        closeJdbc(rs)
      }
    } finally {
      closeJdbc(st)
    }
  }

  def getConnection(driver: String, url: String, user: String, pass: String) = {
    Class.forName(driver)
    DriverManager.getConnection(url, user, pass)
  }

  def getConnection(ds: DataSource) = ds.getConnection
}

