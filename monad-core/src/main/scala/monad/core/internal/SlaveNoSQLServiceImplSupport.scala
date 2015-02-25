// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import monad.core.config.NoSqlConfigSupport
import monad.core.services.SlaveNoSQLService
import monad.jni.services.gen.{NoSQLOptions, SlaveNoSQLSupport}
import monad.support.services.LoggerSupport

/**
 * processor nosql instance
 */
abstract class SlaveNoSQLServiceImplSupport(val config: NoSqlConfigSupport)
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
    noSQLOptions.setMax_open_files(config.noSql.maxOpenFiles)
    noSQLOptions.setCache_size_mb(config.noSql.cache)
    noSQLOptions.setBlock_size_kb(config.noSql.blockSizeKb)
    noSQLOptions.setTarget_file_size(config.noSql.targetFileSize)
    noSQLOptions.setMax_mmap_size(config.noSql.maxMmapSize)
    noSQLOptions.setLog_keeped_num(1000)
    noSQLOptions.setWrite_buffer_mb(config.noSql.writeBuffer)
    slaveNoSQLOpt = Some(createNoSQLInstance(config.noSql.path, noSQLOptions))
    noSQLOptions.delete()

  }
}
