// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * Created by jcai on 14-8-21.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NoSqlConfig")
class NoSqlConfig {
  /** 缓存的大小，此单位为MB **/
  @XmlElement(name = "cache_size_mb")
  var cache: Int = 8
  @XmlElement(name = "block_size_kb")
  var blockSizeKb: Int = 32
  @XmlElement(name = "write_buffer_mb")
  var writeBuffer: Int = 32
  @XmlElement(name = "max_open_files")
  var maxOpenFiles: Int = 1000
  /** 数据库的存放地址 **/
  @XmlElement(name = "path")
  var path: String = _
  @XmlElement(name = "target_file_size_mb")
  var targetFileSize: Int = 32
  @XmlElement(name = "max_mmap_size")
  var maxMmapSize: Int = 64
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NoSqlConfigSupport")
trait NoSqlConfigSupport {
  @XmlElement(name = "nosql")
  var noSql: NoSqlConfig = new NoSqlConfig
}
