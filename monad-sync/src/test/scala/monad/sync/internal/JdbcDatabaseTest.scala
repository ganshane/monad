// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.internal

import java.sql.Connection

import monad.sync.internal.JdbcDatabase._
import org.junit.Test

/**
 * jdbc database test
 * @author jcai
 */
class JdbcDatabaseTest {
  @Test
  def test_jdbc() {
    implicit val conn: Connection = JdbcDatabase.getConnection("org.h2.Driver", "jdbc:h2:mem:test", "sa", "")
    execute("create table test(id integer);")
    update("insert into test values(1);")
    query("select * from test") {
      rs =>
        println(rs.getInt(1))
    }
    closeJdbc(conn)
  }
}
