// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import java.util.UUID

import monad.core.MonadCoreConstants
import monad.core.config.HeartbeatConfigSupport
import monad.core.services.{CronScheduleWithStartModel, MachineHeartbeat, StartAtOnce}
import monad.rpc.config.RpcBindSupport
import monad.support.services.{MonadUtils, ZookeeperTemplate}
import org.apache.tapestry5.ioc.services.cron.{PeriodicExecutor, PeriodicJob}
import org.apache.tapestry5.json.JSONObject

/**
 * machine heartbeat
 */
class MachineHeartbeatImpl(heartbeatConfigSupport: HeartbeatConfigSupport,
                           zk: ZookeeperTemplate,
                           store: LocalSimpleStore,
                           periodicExecutor: PeriodicExecutor) extends MachineHeartbeat {
  private val MACHINE_ID_KEY = "machine_id"
  private val machineId: String = findMachineId()
  private val heartPath = MonadCoreConstants.HEARTBEATS + "/" + machineId
  private val hostName: Option[String] = findHostName
  private val startTime = MonadUtils.currentTimeInSecs
  private val machineInfo = new MachineInfo(hostName, startTime, 0)
  private var jobOpt: Option[PeriodicJob] = None

  /**
   * 得到机器的ID
   * @return 机器ID信息
   */
  override def findMachineId(): String = {
    val idOpt = store.get[String](MACHINE_ID_KEY)
    idOpt match {
      case Some(id) =>
        id
      case None =>
        val id = UUID.randomUUID.toString
        store.put(MACHINE_ID_KEY, id)

        id
    }
  }

  /**
   * 服务关闭
   */
  override def shutdown(): Unit = {
    jobOpt.foreach(_.cancel())
  }

  /**
   * 启动服务
   */
  override def start(): Unit = {
    /*
    //测试十次，每次100毫秒，看是否存在
    val r = 0 until 10 forall {case i=>
      val stat = zk.stat(heartPath)
      if(stat.isDefined){
        Thread.sleep(100)
        true
      }else{
        false
      }
    }
    if(r){//还是存在，则抛出异常
      throw new MonadException("heart path exist["+heartPath+"]!",MonadCoreErrorCode.HEART_PATH_EXISTS)
    }
    */
    zk.createEphemeralPathWithStringData(heartPath)
    update()
    val job = periodicExecutor.addJob(
      new CronScheduleWithStartModel(heartbeatConfigSupport.heartbeat.cron, StartAtOnce),
      "heart-beat",
      new Runnable {
        override def run(): Unit = {
          update()
        }
      }
    )
    jobOpt = Some(job)
  }

  private def update() {
    machineInfo.timeSecs = MonadUtils.currentTimeInSecs
    machineInfo.upSecs = machineInfo.timeSecs - startTime
    zk.setStringData(heartPath, machineInfo.toJSON)
  }

  private def findHostName = {
    var hostName: Option[String] = None

    //优先读取配置
    if (heartbeatConfigSupport.heartbeat.hostName != null)
      hostName = Some(heartbeatConfigSupport.heartbeat.hostName)
    else if (hostName.isEmpty && heartbeatConfigSupport.isInstanceOf[RpcBindSupport])
      hostName = Some(MonadUtils.parseBind(heartbeatConfigSupport.asInstanceOf[RpcBindSupport].rpc.bind)._1)

    //从操作系统读取
    if (hostName.isEmpty) {
      hostName = MonadUtils.hostname
    }

    hostName
  }
}

object MachineInfo {
  private val HOST_NAME_KEY = "host_name"
  private val TIME_SECS_KEY = "time_secs"
  private val UP_SECS_KEY = "up_secs"

  def fromJSON(json: String): MachineInfo = {
    val jsonObj = new JSONObject(json)
    var hostNameOpt: Option[String] = None
    if (jsonObj.has(HOST_NAME_KEY))
      hostNameOpt = Some(jsonObj.getString(HOST_NAME_KEY))
    val timeSecs = jsonObj.getInt(TIME_SECS_KEY)
    val upSecs = jsonObj.getInt(UP_SECS_KEY)
    new MachineInfo(hostNameOpt, timeSecs, upSecs)
  }
}

class MachineInfo(val hostName: Option[String], var timeSecs: Int, var upSecs: Int) {
  def toJSON: String = {
    val json = new JSONObject()
    if (hostName.isDefined)
      json.put(MachineInfo.HOST_NAME_KEY, hostName.get)
    json.put(MachineInfo.TIME_SECS_KEY, timeSecs)
    json.put(MachineInfo.UP_SECS_KEY, upSecs)
    json.toCompactString
  }
}
