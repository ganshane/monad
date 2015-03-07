// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.io.File

import monad.core.config.NoSqlConfig
import monad.face.model.ResourceDefinition
import monad.jni.services.gen.{NoSQLOptions, SlaveNoSQLSupport}
import monad.node.services.SlaveNoSQLService
import monad.support.services.LoggerSupport
import org.apache.commons.io.FileUtils

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
  def startNoSQLInstance(rd: ResourceDefinition): Unit = {
    info("start processor nosql instance")
    val noSQLOptions = new NoSQLOptions()
    noSQLOptions.setMax_open_files(config.maxOpenFiles)
    noSQLOptions.setCache_size_mb(config.cache)
    noSQLOptions.setBlock_size_kb(config.blockSizeKb)
    noSQLOptions.setTarget_file_size(config.targetFileSize)
    noSQLOptions.setMax_mmap_size(config.maxMmapSize)
    noSQLOptions.setLog_keeped_num(1000)
    noSQLOptions.setWrite_buffer_mb(config.writeBuffer)
    slaveNoSQLOpt = Some(createNoSQLInstance(config.path + "/" + rd.name, noSQLOptions))
    noSQLOptions.delete()

  }

  def destryNoSQL(rd: ResourceDefinition): Unit = {
    val originPath = new File(config.path + "/" + rd.name)
    val tmpPath = new File(config.path + "/" + rd.name + "." + System.currentTimeMillis())
    FileUtils.moveDirectory(originPath, tmpPath)
    FileUtils.deleteQuietly(tmpPath)
  }
}
