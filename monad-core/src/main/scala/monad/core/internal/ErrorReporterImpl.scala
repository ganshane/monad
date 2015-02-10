// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import java.io.{PrintWriter, StringWriter}

import monad.core.MonadCoreConstants
import monad.core.services.{ErrorReporter, MachineHeartbeat}
import monad.support.services.{MonadUtils, ZookeeperTemplate}

/**
 * error reporter
 */
class ErrorReporterImpl(heartbeat: MachineHeartbeat,
                        zk: ZookeeperTemplate) extends ErrorReporter {
  private val machineErrorPath = MonadCoreConstants.ERRORS + "/" + heartbeat.findMachineId()
  zk.createPersistPath(machineErrorPath)

  /**
   * 报告系统异常消息
   * @param error 系统异常消息
   */
  override def report(error: Throwable): Unit = {
    //先查找所有的子节点
    val children = zk.getChildren(machineErrorPath)

    //保留10个节点
    children.map(_.toInt).sorted.dropRight(10).foreach {
      timeNode =>
        zk.delete(machineErrorPath + "/" + timeNode)
    }

    val now = MonadUtils.currentTimeInSecs
    val sw: StringWriter = new StringWriter
    val pw: PrintWriter = new PrintWriter(sw)
    error.printStackTrace(pw)
    zk.createPersistPathWithString(machineErrorPath + "/" + now, Some(sw.toString))
  }
}
