// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import monad.core.services.MonadCoreErrorCode
import stark.rpc.config.RpcBindSupport
import monad.support.services.{MonadException, MonadUtils}

/**
 * utils method
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
object MonadCoreUtils {
  @deprecated(message="use readConfigContent",since = "5.0.12")
  def findConfigPath(serverHome: String, configFile: String) = {
    readConfigContent(serverHome,configFile)
  }
  def readConfigContent(serverHome: String, configFile: String) = {
    MonadConfigFileUtils.readConfigFileContent(serverHome, configFile)
  }

  def autoFixIpAddress[T](rpcBind: T) = {
    rpcBind match {
      case bindSupport: RpcBindSupport =>
        val bind = bindSupport.rpc.bind
        val (ip, port) = MonadUtils.parseBind(bind)
        val ipReal = MonadUtils.ip(ip)
        ipReal match {
          case Some((address, mac)) =>
            bindSupport.rpc.bind = "%s:%s".format(address, port)
          case None =>
            throw new MonadException("ip address not found by pattern %s".format(ip), MonadCoreErrorCode.IP_NOT_FOUND)
        }
      case _ =>
    }

    rpcBind
  }
}
