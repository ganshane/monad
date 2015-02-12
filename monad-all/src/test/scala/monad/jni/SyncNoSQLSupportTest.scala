package monad.jni

import org.junit.{Assert, Test}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
class SyncNoSQLSupportTest {
  @Test
  def test_sync_nosql {
    JniLoader.loadJniLibrary("support")
    val sync1 = new SyncNoSQL("target/test_sync", new NoSQLOptions())
    sync1.AddRegion(1, 2)
    val idSync1 = new SyncIdNoSQL("target/test_sync_id", new NoSQLOptions())
    idSync1.AddRegion(1)
    sync1.SetSyncIdNoSQL(idSync1)
    sync1.Put2("asdf".getBytes(), "fdsa".getBytes(), 1, "asdf".getBytes());
    sync1.Put2("fdsa".getBytes(), "fdsa".getBytes(), 1, null);
    /*
    val sync2 = new SyncNoSQLSupport("target/test_sync_2",1,1)
    sync2.AddServer("s1",2)
    sync2.Put("asdf".getBytes(),"fdsa".getBytes(),1);
    sync2.Put("fdsa".getBytes(),"fdsa".getBytes(),1);
    */
    Assert.assertEquals(2, sync1.GetDataStatCount())

    sync1.delete()
    //sync2.delete()
  }
}
