// Copyright 2012,2013,2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.LockSupport

import org.apache.commons.io.FileUtils
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.apache.zookeeper.server.{ServerCnxnFactory, ServerConfig, ZooKeeperServer}
import org.junit._
import org.mockito.Mockito
import org.slf4j.LoggerFactory


/**
 *
 * @author jcai
 */

class ZookeeperTemplateTest {
  private val logger = LoggerFactory getLogger getClass
  private var cnxnFactory: ServerCnxnFactory = _

  @Test
  def test_fail_create_path() {
    val zookeeper = new ZookeeperTemplate("localhost:2888")
    start(zookeeper)

    zookeeper.testCreateFailedEphemeral("/c")

    zookeeper.retry()

    val stat = zookeeper.stat("/c")
    Assert.assertTrue(stat.isDefined)


    zookeeper.shutdown()
  }

  @Test
  def test_session_timeout() {
    val zookeeper = new ZookeeperTemplate("localhost:2888", sessionTimeout = 2000)
    start(zookeeper)

    zookeeper.createPersistPath("/c", Some("asdf".getBytes("UTF-8")))
    zookeeper.createEphemeralPath("/ca", Some("asdf".getBytes("UTF-8")))
    zookeeper.createEphemeralPath("/a/b/c")
    Assert.assertEquals("asdf", zookeeper.getDataAsString("/ca").get)
    val zk = zookeeper.buildAnotherSession
    zk.close()

    Thread.sleep(3000)
    Assert.assertEquals("asdf", zookeeper.getDataAsString("/c").get)
    Assert.assertEquals("asdf", zookeeper.getDataAsString("/ca").get)
    Assert.assertTrue(zookeeper.stat("/a/b/c").isDefined)
    zookeeper.shutdown()
  }

  @Test
  def test_watch_children() {
    val zookeeper = new ZookeeperTemplate("localhost:2888")
    start(zookeeper)

    val assignLatch = new CountDownLatch(1)
    @volatile
    var a = 0
    var r: Seq[String] = Seq[String]()
    zookeeper.watchChildren("/c", new ChildrenDataWatcher {
      def handleDataChanged(data: Seq[String]) {
        println("watched... " + data)
        a += 1
        r = data
        if (a == 2) throw new RuntimeException("test exception")
        assignLatch.countDown()
      }
    })
    assignLatch.await()

    zookeeper.createPersistPath("/c", Some("asdf".getBytes))
    //Thread.sleep(2000)
    logger.debug("creating /c/a")
    zookeeper.createPersistPath("/c/a")
    //Thread.sleep(1000)
    //Assert.assertEquals(2,a)
    logger.debug("creating /c/b")
    zookeeper.createPersistPath("/c/b")
    Thread.sleep(1000)
    //Assert.assertEquals(3,a)
    logger.debug("creating /c/b")
    zookeeper.createPersistPath("/c/b", data = Some("asdf".getBytes))
    //Assert.assertEquals(3,a)
    //删除之后
    logger.debug("deleting /c/a")
    zookeeper.delete("/c/a")
    //Thread.sleep(2000)
    Thread.sleep(5000)
    Assert.assertEquals(1, r.size)
    zookeeper.delete("/c/b")
    Thread.sleep(5000)
    Assert.assertEquals(0, r.size)

    zookeeper.delete("/c")
    Thread.sleep(5000)
    Assert.assertEquals(0, r.size)

    zookeeper.createPersistPath("/c/a")
    Thread.sleep(5000)
    Assert.assertEquals(1, r.size)

    zookeeper.shutdown()
  }

  @Test
  def test_watch() {
    val zookeeper = new ZookeeperTemplate("localhost:2888")
    start(zookeeper)

    zookeeper.createPersistPath("/c", Some("asdf".getBytes("UTF-8")))
    var a = 0
    val assignLatch = new CountDownLatch(1)
    @volatile
    var r: Option[String] = None

    zookeeper.watchNodeData("/c",
      new NodeDataWatcher {
        def handleNodeDeleted() {
          r = None
        }

        def handleDataChanged(data: Option[Array[Byte]]) {
          println("watched... " + data.map(new String(_)))
          a += 1
          assignLatch.countDown()
          r = data.map(new String(_))
        }
      })
    assignLatch.await()
    zookeeper.setStringData("/c", "hehe")
    zookeeper.setStringData("/c", "fdsa")
    zookeeper.setStringData("/c", "fdsa")
    zookeeper.setStringData("/c", "fdsa")
    zookeeper.setStringData("/c", "fdsa")
    zookeeper.setStringData("/c", "fdsa")
    zookeeper.setStringData("/c", "cctv")

    LockSupport.parkNanos(10L * 1000000000)
    Assert.assertEquals("cctv", r.get)
    //Assert.assertEquals(2,a)

    val zk = zookeeper.buildAnotherSession
    zk.close()
    Thread.sleep(5000)

    zookeeper.setStringData("/c", "fdsa")
    //Thread.sleep(1000)
    //Assert.assertEquals(4,a)

    zookeeper.delete("/c")
    Thread.sleep(5000)
    Assert.assertEquals(None, r)

    zookeeper.shutdown()
  }

  private def start(zk: ZookeeperTemplate): Unit = {
    zk.start(Mockito.mock(classOf[RegistryShutdownHub]))
  }

  @Test
  def test_create_path() {
    val zookeeper = new ZookeeperTemplate("localhost:2888")
    start(zookeeper)
    Assert.assertTrue(zookeeper.getData("/x").isEmpty)

    Assert.assertTrue(zookeeper.stat("/a/b/c/d").isEmpty)
    zookeeper.createPersistPath("/a/b/c/d")
    Assert.assertTrue(zookeeper.stat("/a/b/c/d").isDefined)
    zookeeper.deleteRecursive("/a/b")
    Assert.assertTrue(zookeeper.stat("/a/b").isEmpty)
    Assert.assertTrue(zookeeper.stat("/a").isDefined)

    Assert.assertTrue(zookeeper.stat("/b").isEmpty)
    zookeeper.createEphemeralPath("/b")
    Assert.assertTrue(zookeeper.stat("/b").isDefined)

    Assert.assertTrue(zookeeper.getData("/a") == None)

    zookeeper.createEphemeralPath("/c", Some("asdf".getBytes("UTF-8")))
    Assert.assertEquals("asdf", new String(zookeeper.getData("/c").get))
    zookeeper.setStringData("/c", "超级菜")
    Assert.assertEquals("超级菜", zookeeper.getDataAsString("/c").get)
    zookeeper.delete("/c")
    zookeeper.createPersistPath("/c")
    Assert.assertEquals(zookeeper.getData("/c"), None)

    zookeeper.createPersistPath("/x/y/z", data = Some("fdsa".getBytes))
    Assert.assertEquals("fdsa", zookeeper.getDataAsString("/x/y/z").get)


    zookeeper.shutdown()
  }

  @Test
  def test_stat() {
    val zookeeper = new ZookeeperTemplate("localhost:2888")
    start(zookeeper)

    val stat = new Stat
    stat.setVersion(-1)
    var data = zookeeper.getData("/r-1", Some(stat))
    Assert.assertEquals(-1, stat.getVersion)
    zookeeper.createPersistPath("/r-1")
    data = zookeeper.getData("/r-1", Some(stat))
    Assert.assertEquals(0, stat.getVersion)
    zookeeper.setData("/r-1", "asdf".getBytes, Some(stat))
    val stat2 = new Stat
    stat2.setVersion(-1)
    zookeeper.getData("/r-1", Some(stat2))
    Assert.assertEquals(1, stat2.getVersion)
    try {
      zookeeper.setData("/r-1", "fdsa".getBytes, Some(stat))
      Assert.fail("can't reach this")
    } catch {
      case e: KeeperException.BadVersionException =>
    }
    zookeeper.setData("/r-1", "fdsa".getBytes, Some(stat2))

    zookeeper.shutdown()
  }

  @Test
  def test_children() {
    val root = new ZookeeperTemplate("127.0.0.1:2888")
    start(root)

    root.createPersistPath("/x")
    root.shutdown()
    val zookeeper = new ZookeeperTemplate("127.0.0.1:2888", Some("/x"))
    start(zookeeper)

    zookeeper.createPersistPath("/d/a")
    zookeeper.createPersistPath("/d/b")
    zookeeper.createPersistPath("/d/c")
    zookeeper.createPersistPath("/d/d/f")
    val seq = zookeeper.getChildren("/d")
    Assert.assertEquals(4, seq.size)
    Assert.assertTrue(seq.contains("a"))
    Assert.assertTrue(seq.contains("b"))
    Assert.assertTrue(seq.contains("c"))
    Assert.assertTrue(seq.contains("d"))
    zookeeper.shutdown()
  }

  @Before
  def setupEnv() {
    val config = new ServerConfig()
    config.parse(Array("2888", "target/data"))

    val zkServer = new ZooKeeperServer()

    val file = new File(config.getDataDir)
    FileUtils.deleteQuietly(file)
    file.mkdir()
    val ftxn = new FileTxnSnapLog(new File(config.getDataDir), new File(config.getDataLogDir))
    zkServer.setTxnLogFactory(ftxn)
    zkServer.setTickTime(config.getTickTime)
    zkServer.setMinSessionTimeout(config.getMinSessionTimeout)
    zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout)
    cnxnFactory = ServerCnxnFactory.createFactory()
    cnxnFactory.configure(config.getClientPortAddress,
      config.getMaxClientCnxns)
    cnxnFactory.startup(zkServer)
  }

  @After
  def closeEnv() {
    cnxnFactory.shutdown()
  }
}
