// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import monad.rpc.model.RpcServerLocation
import monad.rpc.services.{MonadRpcErrorCode, RpcServerFinder}
import monad.support.services.{ChildrenDataWatcher, LoggerSupport, MonadException, ZookeeperTemplate}
import org.apache.tapestry5.json.JSONObject

/**
 * implements server finder
 * TODO offline or cache the server info
 */
class RpcServerFinderWithZk(zk: ZookeeperTemplate) extends RpcServerFinder with LoggerSupport {
  private val servicesCache = new ConcurrentHashMap[String, Set[RpcServerLocation]]()
  private val lock = new ReentrantLock()

  /**
   * 查找多个服务器，以pathPrefix开头
   * @param pathPrefix 路径开头
   * @return 服务器地址
   */
  override def findMulti(pathPrefix: String): Array[RpcServerLocation] = {
    var seq = servicesCache.get(pathPrefix)
    if (seq == null) {
      try {
        lock.lock()
        seq = servicesCache.get(pathPrefix)
        if (seq == null) {
          val children = zk.getChildren(pathPrefix)
          processChildren(pathPrefix, children)

          zk.watchChildren(pathPrefix,
            new ChildrenDataWatcher {
              def handleDataChanged(data: Seq[String]) {
                processChildren(pathPrefix, data)
                info("latest service locations:{} for service [{}]", servicesCache.get(pathPrefix), pathPrefix)
              }
            })

          seq = servicesCache.get(pathPrefix)
        }
      } finally {
        lock.unlock()
      }
    }

    if (seq.isEmpty) {
      throw new MonadException("server not found :%s".format(pathPrefix), MonadRpcErrorCode.SERVER_NOT_FOUND)
    }

    seq.toArray
  }

  private def processChildren(pathPrefix: String, data: Seq[String]): Unit = {
    val locations = data.flatMap { child =>
      val serverPath = pathPrefix + "/" + child
      find(serverPath)
    }
    info("latest service locations:{} for service [{}]", locations, pathPrefix)
    servicesCache.put(pathPrefix, locations.toSet)
  }

  override def find(path: String): Option[RpcServerLocation] = {
    val strOpt = zk.getDataAsString(path)
    strOpt match {
      case Some(str) =>
        Some(RpcServerLocation.fromJSON(new JSONObject(str)))
      case None =>
        None
    }
  }

}
