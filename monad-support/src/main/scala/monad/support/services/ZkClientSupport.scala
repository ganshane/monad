// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct

import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.tapestry5.ioc.services.RegistryShutdownHub
import org.apache.tapestry5.ioc.services.cron.{IntervalSchedule, PeriodicExecutor}

/**
 * zookeeper的客户端
 */
trait ZkClientSupport extends ServiceWaitingInitSupport {
  this: ZookeeperTemplate =>

  private final val lock = new ReentrantLock()
  private var client: Option[CuratorFramework] = None

  /**
   * 得到Zookeeper的客户端
   * @return zookeeper client
   */
  def zkClient: CuratorFramework = {
    awaitServiceInit()
    client.getOrElse {
      throw new MonadException("zk client not be initialized", MonadSupportErrorCode.ZK_NOT_BE_INITIALIZED)
    }
  }

  /**
   * 启动对象实例
   */

  @PostConstruct
  def start(hub: RegistryShutdownHub) {
    throwExceptionIfServiceInitialized()

    val builder = CuratorFrameworkFactory.builder()
      .connectString(address)
      .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000))
      .connectionTimeoutMs(sessionTimeout).
      defaultData(null)
    if (basePath.isDefined) {
      if (basePath.get.charAt(0) == '/') {
        //curator中的namespace前端没有 / 开头
        builder.namespace(basePath.get.substring(1))
      } else {
        builder.namespace(basePath.get)
      }
    }

    val tmpClient = builder.build()
    //增加统一的状态监听，方便对watcher进行再次监控
    tmpClient.getConnectionStateListenable.addListener(new ConnectionStateListener() {
      def stateChanged(client: CuratorFramework, state: ConnectionState) {
        try {
          //不支持多线程进行watch，避免过多的watch
          lock.lock()
          doStateChanged(state)
        } finally {
          lock.unlock()
        }
      }
    })
    tmpClient.start()

    client = Some(tmpClient)

    serviceInitialized()

    if (hub != null) {
      hub.addRegistryShutdownListener(new Runnable {
        override def run(): Unit = shutdown()
      })
    }
  }

  private def doStateChanged(state: ConnectionState) {
    if (state == ConnectionState.CONNECTED) {
      //第一次连接
      //connectedFun.apply(this)
    } else if (state == ConnectionState.RECONNECTED) {
      //重新连接上，通常是session过期造成的
      info("cloud server reconnected")
      //针对临时节点的再次创建
      recreateEphemeralNodes()

      rewatchNodeData()
      rewatchChildren()

      //connectedFun.apply(this)
    }
  }

  /**
   * 关闭对象
   */
  def shutdown() {
    debug("closing zk client....")
    client.foreach(_.close)
  }

  /**
   * 启动对异常数据的检测,实现FailBack功能
   * @param periodExecutor 定时执行器
   */
  def startCheckFailed(periodExecutor: PeriodicExecutor) {
    if (periodExecutor == null) {
      error("periodExecutor is null!")
      return
    }
    //启动定时器
    periodExecutor.addJob(new IntervalSchedule(1L * 60 * 1000), "check-zk", new Runnable {
      def run() {
        retry()
      }
    })
  }

  //尝试修正失败的数据
  private[monad] def retry() {
    retryFailedEphemeralNodes()
    retryFailedWatchNodeData()
    retryFailedChildrenWatcher()
  }

  sealed class WatchMode

  case object OnceWatch extends WatchMode

  case object PermanentWatch extends WatchMode

}
