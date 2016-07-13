// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.group.internal

import java.io.File

import monad.face.model.{DynamicResourceDefinition, ResourceRelation}
import monad.face.services.{GroupServerApi, GroupZookeeperTemplate}
import monad.group.config.MonadGroupConfig
import org.apache.commons.io.FileUtils
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.apache.zookeeper.server.{ServerCnxnFactory, ServerConfig, ZooKeeperServer}
import org.junit.{After, Assert, Before, Test}
import org.mockito.Mockito
import roar.api.meta.ResourceDefinition
import stark.utils.services.XmlLoader

/**
 * monad group manager
 *
 * @author jcai
 */
class MonadGroupManagerTest {
  private var cnxnFactory: ServerCnxnFactory = _

  @Before
  def setupEnv() {
    val config = new ServerConfig()
    config.parse(Array("2888", "target/data"))

    val zkServer = new ZooKeeperServer()

    val file = new File(config.getDataDir)
    FileUtils.deleteQuietly(file)
    file.mkdir()
    var ftxn = new FileTxnSnapLog(new File(config.getDataDir), new File(config.getDataLogDir))
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

  @Test
  def test_saveOrUpdateResource() {
    Thread.sleep(1000)
    val config = new MonadGroupConfig
    config.zk.address = "127.0.0.1:2888"
    config.group.id = "cd"
    config.group.cnName = "中文名称"
    val clusterManager = new MonadGroupUpNotifier(config, null, null)
    val groupApi = Mockito.mock(classOf[GroupServerApi])
    Mockito.when(groupApi.GetCloudAddress).thenReturn(config.zk.address)
    Mockito.when(groupApi.GetSelfGroupConfig).thenReturn(config.group)
    val zk = new GroupZookeeperTemplate(groupApi, null)
    zk.start(Mockito.mock(classOf[RegistryShutdownHub]))

    val manager = new MonadGroupManager(config, zk)
    val resource = new ResourceDefinition
    resource.name = "wbswry"
    manager.saveOrUpdateResource(resource)
    resource.cnName = "修改后的名字"
    manager.saveOrUpdateResource(resource)
    val rd = XmlLoader.parseXML[ResourceDefinition](manager.getResource("wbswry"))
    Assert.assertEquals("修改后的名字", rd.cnName)

    Assert.assertFalse(zk.stat("/resources/wbswry") == None)

    val wbswryContent = zk.getData("/resources/wbswry")
    Assert.assertTrue(wbswryContent != None)
    Assert.assertNotNull(wbswryContent.get)

    manager.saveOrUpdateDynamic(new DynamicResourceDefinition(), Some("<xml>asdf</xml>"))
    Assert.assertEquals("<xml>asdf</xml>", manager.getDynamic)
    Assert.assertEquals("", manager.getRelation)
    manager.saveOrUpdateRelation(new ResourceRelation(), Some("<xml>fdsa</xml>"))
    Assert.assertEquals("<xml>fdsa</xml>", manager.getRelation)

    zk.shutdown
  }
}
