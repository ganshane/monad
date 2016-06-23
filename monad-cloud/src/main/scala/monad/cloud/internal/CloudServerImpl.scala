// Copyright 2012,2013,2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud.internal

import java.io.File
import java.util.Properties
import javax.annotation.PostConstruct

import monad.cloud.config.MonadCloudConfig
import monad.cloud.services.CloudServer
import monad.core.MonadCoreConstants
import stark.utils.services.{LoggerSupport, ZookeeperTemplate}
import org.apache.commons.io.FileUtils
import org.apache.tapestry5.ioc.annotations.EagerLoad
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.zookeeper.server._
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.apache.zookeeper.server.quorum.{QuorumPeer, QuorumPeerConfig}

import scala.collection.JavaConversions._

/**
 * 集群服务器
 * @author jcai
 */
@EagerLoad
class CloudServerImpl(clusterConfig: MonadCloudConfig)
  extends CloudServer
  with LoggerSupport {

  private var server: Stopper = null

  @PostConstruct
  def start(hub: RegistryShutdownHub) {
    val config = readConfig(clusterConfig)
    // Start and schedule the the purge task
    val purgeMgr = new DatadirCleanupManager(config
      .getDataDir, config.getDataLogDir, config
      .getSnapRetainCount, 2)
    //.getSnapRetainCount(), config.getPurgeInterval());
    purgeMgr.start()


    server =
      if (config.getServers.size() > 0) {
        //配置了多个服务器，则启动集群服务器
        startQuorumPeer(config)
      } else {
        //启动标准的单一服务器
        val serverConfig = new ServerConfig
        serverConfig.readFrom(config)
        startStandardServer(serverConfig)
        /*
        val standardMain = new ZooKeeperServerMain() with Stopper{
            def stop = {
                this.shutdown()
            }
        }
        standardMain.runFromConfig(serverConfig)
        standardMain
        */
      }
    setupMonadDirectory()


    hub.addRegistryWillShutdownListener(new Runnable {
      override def run(): Unit = shutdown()
    })
  }

  private def startStandardServer(config: ServerConfig) = new Stopper {
    private val zkServer = new ZooKeeperServer()
    private val ftxn = new FileTxnSnapLog(new
        File(config.getDataLogDir), new File(config.getDataDir))
    zkServer.setTxnLogFactory(ftxn)
    zkServer.setTickTime(config.getTickTime)
    zkServer.setMinSessionTimeout(config.getMinSessionTimeout)
    zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout)
    private val cnxnFactory = ServerCnxnFactory.createFactory()
    cnxnFactory.configure(config.getClientPortAddress,
      config.getMaxClientCnxns)
    cnxnFactory.startup(zkServer)

    def stop() {
      cnxnFactory.shutdown()
    }
  }

  //启动集群服务器
  private def startQuorumPeer(config: QuorumPeerConfig) = new Stopper {
    private val cnxnFactory = ServerCnxnFactory.createFactory()
    try {
      cnxnFactory.configure(config.getClientPortAddress, config.getMaxClientCnxns)

      val quorumPeer = new QuorumPeer()
      quorumPeer.setClientPortAddress(config.getClientPortAddress)
      quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config.getDataLogDir), new File(config.getDataDir)))
      quorumPeer.setQuorumPeers(config.getServers)
      quorumPeer.setElectionType(config.getElectionAlg)
      quorumPeer.setMyid(config.getServerId)
      quorumPeer.setTickTime(config.getTickTime)
      quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout)
      quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout)
      quorumPeer.setInitLimit(config.getInitLimit)
      quorumPeer.setSyncLimit(config.getSyncLimit)
      quorumPeer.setQuorumVerifier(config.getQuorumVerifier)
      quorumPeer.setCnxnFactory(cnxnFactory)
      quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory))
      quorumPeer.setLearnerType(config.getPeerType)

      quorumPeer.start()
    } catch {
      case ie: InterruptedException =>
        // warn, but generally this is ok
        warn("Quorum Peer interrupted", ie)
    }

    def stop() {
      cnxnFactory.shutdown()
    }
  }

  private def readConfig(clusterConfig: MonadCloudConfig) = {
    val config = new QuorumPeerConfig()
    val zkProp = new Properties
    zkProp.put("dataDir", clusterConfig.dataDir)
    val dataDir = new File(clusterConfig.dataDir)
    FileUtils.forceMkdir(dataDir)
    FileUtils.writeByteArrayToFile(new File(dataDir, "myid"),
      clusterConfig.myId.toString.getBytes)
    zkProp.put("dataLogDir", clusterConfig.dataDir)
    zkProp.put("initLimit", clusterConfig.initLimit.toString)
    zkProp.put("syncLimit", clusterConfig.syncLimit.toString)
    zkProp.put("clientPort", clusterConfig.port.toString)
    clusterConfig.servers.foreach { s =>
      zkProp.put("server." + s.id, s.address)
    }
    config.parseProperties(zkProp)

    config
  }

  private def setupMonadDirectory() {
    val rootZk = new ZookeeperTemplate("localhost:" + clusterConfig.port)
    rootZk.start(null)

    //Copyr from
    rootZk.createPersistPath(MonadCoreConstants.ROOT_PATH + MonadCoreConstants.GROUPS_PATH)
    rootZk.createPersistPath(MonadCoreConstants.ROOT_PATH + MonadCoreConstants.LIVE_PATH)

    rootZk.shutdown()
  }

  def shutdown() {
    if (server != null) {
      info("Closing cloud server....")
      server.stop()
    }
  }

  trait Stopper {
    def stop()
  }

}
