// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import monad.core.config.{NoSqlConfigSupport, PartitionIdSupport}
import monad.core.services.{DataSynchronizerSupport, SlaveNoSQLService}
import monad.jni.services.gen.{NoSQLOptions, SlaveNoSQLSupport}
import monad.rpc.services.{RpcClient, RpcServerFinder}
import monad.support.services.LoggerSupport
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor

/**
 * processor nosql instance
 */
abstract class SlaveNoSQLServiceImplSupport(masterPath: String,
                                            val config: NoSqlConfigSupport,
                                            partitionIdSupport: PartitionIdSupport,
                                            rpcClient: RpcClient,
                                            periodicExecutor: PeriodicExecutor,
                                            rpcServerFinder: RpcServerFinder)
  extends SlaveNoSQLService
  with DataSynchronizerSupport
  with LoggerSupport {

  private var slaveNoSQLOpt: Option[SlaveNoSQLSupport] = None
  private val partitionsData = Array[Short] {
    partitionIdSupport.partitionId
  }

  override def findNoSQLByPartitionId(partitionId: Short): Option[SlaveNoSQLSupport] = slaveNoSQLOpt

  override def getPartitionData: Array[Short] = partitionsData

  override def nosql(): Option[SlaveNoSQLSupport] = slaveNoSQLOpt

  /**
   * 服务关闭
   */
  def shutdownNoSQLInstance(): Unit = {
    shutdownSynchronizer()
    info("shutdown nosql instance")
    slaveNoSQLOpt.foreach(_.delete())
  }

  protected def createNoSQLInstance(path: String, noSQLOption: NoSQLOptions): SlaveNoSQLSupport

  /**
   * 启动服务
   */
  def startNoSQLInstance(): Unit = {
    info("start processor nosql instance")
    val noSQLOptions = new NoSQLOptions()
    noSQLOptions.setMax_open_files(config.noSql.maxOpenFiles)
    noSQLOptions.setCache_size_mb(config.noSql.cache)
    noSQLOptions.setBlock_size_kb(config.noSql.blockSizeKb)
    noSQLOptions.setTarget_file_size(config.noSql.targetFileSize)
    noSQLOptions.setMax_mmap_size(config.noSql.maxMmapSize)
    noSQLOptions.setLog_keeped_num(1000)
    noSQLOptions.setWrite_buffer_mb(config.noSql.writeBuffer)
    slaveNoSQLOpt = Some(createNoSQLInstance(config.noSql.path, noSQLOptions))
    noSQLOptions.delete()

    startSynchronizer(masterPath, periodicExecutor, rpcClient)
  }
}
