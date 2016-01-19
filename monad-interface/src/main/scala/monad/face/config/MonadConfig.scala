// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.config

import java.util
import javax.xml.bind.annotation._

import monad.core.config.{NoSqlConfig, Partition, PartitionIdSupport}
import monad.face.model.GroupConfig

/**
 * monad 全局配置
 * @author jcai
 */

trait ServerIdSupport {
  @XmlElement(name = "data_region_id")
  var regionId: Short = _
}

@XmlType(name = "GroupConfig")
trait GroupConfigSupport {
  @XmlElement(name = "group")
  var group: GroupConfig = new GroupConfig
}

trait ExtjsSupport {
  /* 日志所在目录 */
  @XmlElement(name = "extjs_dir")
  var extjsDir: String = _
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Region")
class Region {
  @XmlAttribute(name = "id")
  var id: Short = _
  @XmlAttribute(name = "weight")
  var weight: Int = 1
}

@XmlType(name = "BinlogLengthConfig")
class BinlogLengthConfig {
  @XmlElement(name = "binlog_length")
  var binlogLength: Int = 1000
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SyncConfig")
class SyncConfig extends BinlogLengthConfig {
  //同步服务器绑定的地址
  @XmlElement(name = "bind")
  var bind: String = "tcp://*:7777"
  @XmlElement(name = "db_reader_num")
  var db_thread_num: Int = 5
  @XmlElement(name = "sync_server_worker")
  var sync_thread_num: Int = 1
  //是否忽略不合格的数据，true为忽略数据，false 为不忽略
  @XmlElement(name = "ignore_row_when_unqualified_field")
  var ignore_data_when_unqualified_field: Boolean = false
  @XmlElementWrapper(name = "data")
  @XmlElement(name = "partition")
  var nodes: java.util.List[Partition] = new util.ArrayList[Partition]()
  @XmlElement(name = "nosql")
  var noSql: NoSqlConfig = new NoSqlConfig
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SyncConfigSupport")
class SyncConfigSupport {
  @XmlElement(name = "sync")
  var sync: SyncConfig = new SyncConfig()
}

@XmlAccessorType(XmlAccessType.FIELD)
trait NoSqlConfigSupport {
  @XmlElement(name = "nosql")
  var noSql: NoSqlConfig = new NoSqlConfig
}

@XmlAccessorType(XmlAccessType.FIELD)
trait IdConfigSupport {
  @XmlElement(name = "id_server")
  var idServerConfig: NoSqlConfig = new NoSqlConfig
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ApiConfig")
class ApiConfig {
  @XmlElement(name = "enable_memcached")
  var enableMemcachedCache = false
  @XmlElement(name = "memcached_servers")
  var memcachedServers: String = _
  //缓存过期时间
  @XmlElement(name = "expired_period_in_minutes")
  var expiredPeriodInMinutes: Int = 360

  //并发查询的数目
  @XmlElement(name = "concurrent_query")
  var concurrentQuery: Int = 8
}

@XmlAccessorType(XmlAccessType.FIELD)
trait ApiConfigSupport {
  @XmlElement(name = "api")
  var api: ApiConfig = new ApiConfig
}

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "IndexConfig")
class IndexConfig extends BinlogLengthConfig {
  /** 索引所在目录 **/
  @XmlElement(name = "path")
  var path: String = "target/index-tmp"
  /** 进行索引写操作时候的内存缓存,此数据单位为 MB **/
  @XmlElement(name = "writer_buffer")
  var writerBuffer: Int = 32
  /** 需要进行提交时候的数量 **/
  @XmlElement(name = "need_commit")
  var needCommit: Int = 100000
  @XmlElement(name = "max_bytes_per_sec")
  var maxBytesPerSec: Double = 0
  //是否开启多线程查询支持
  @XmlElement(name = "query_thread")
  var queryThread = 1
  @XmlElement(name = "query_max_limit")
  var queryMaxLimit = -1
  @XmlElement(name = "query_cache_supported")
  var queryCacheSupported= true

  //保存id
  //@XmlElement(name = "id_nosql")
  //var idNoSql: NoSqlConfig = _
}

trait IndexConfigSupport extends PartitionIdSupport with monad.core.config.NoSqlConfigSupport {
  @XmlElement(name = "index")
  var index: IndexConfig = new IndexConfig
}

trait DicPathSupport {
  @XmlElement(name = "dic_dir")
  var dicDir: String = _
}

trait GroupApiSupport {
  @XmlElement(name = "group_api")
  var groupApi: String = _
}
