package monad.jni.services.gen

import org.junit.{Assert, Test}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-05-24
 */
class SlaveNoSQLSupportTest extends BaseJniTestCase {
  @Test
  def test_binlog: Unit = {
    val option = new NoSQLOptions()
    val slaveNoSQL = new SlaveNoSQLSupport("target/slave", option)
    for (i <- 1 until 1000) {
      val binlogValue = new SyncBinlogValue(1, i, DataCommandType.PUT,
        new NoSQLKey("asdf".getBytes()), "fdsa".getBytes())

      slaveNoSQL.PutBinlog(binlogValue)
    }

    slaveNoSQL.DeleteBinlogRange(1, 100);
    var binlog = slaveNoSQL.FindNextBinlog(1)
    Assert.assertEquals(101, binlog.Seq())
    binlog = slaveNoSQL.FindNextBinlog(101)
    Assert.assertEquals(101, binlog.Seq())
  }
}
