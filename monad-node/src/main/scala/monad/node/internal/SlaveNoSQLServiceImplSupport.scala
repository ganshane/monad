// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import monad.core.config.NoSqlConfig
import monad.jni.services.gen.{NoSQLOptions, SlaveNoSQLSupport}
import monad.node.services.SlaveNoSQLService
import monad.support.services.LoggerSupport

/**
 * processor nosql instance
 */
abstract class SlaveNoSQLServiceImplSupport(val config: NoSqlConfig)
  extends SlaveNoSQLService
  with LoggerSupport {

  private var slaveNoSQLOpt: Option[SlaveNoSQLSupport] = None


  override def nosqlOpt(): Option[SlaveNoSQLSupport] = slaveNoSQLOpt

  /**
   * 服务关闭
   */
  def shutdownNoSQLInstance(): Unit = {
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
    noSQLOptions.setMax_open_files(config.maxOpenFiles)
    noSQLOptions.setCache_size_mb(config.cache)
    noSQLOptions.setBlock_size_kb(config.blockSizeKb)
    noSQLOptions.setTarget_file_size(config.targetFileSize)
    noSQLOptions.setMax_mmap_size(config.maxMmapSize)
    noSQLOptions.setLog_keeped_num(1000)
    noSQLOptions.setWrite_buffer_mb(config.writeBuffer)
    slaveNoSQLOpt = Some(createNoSQLInstance(config.path, noSQLOptions))
    noSQLOptions.delete()

  }
}
