// Copyright 2012,2013,2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud.config

import java.util
import javax.xml.bind.annotation._

import monad.core.config.LogFileSupport

/**
 * monad cloud config
 * @author jcai
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MonadCloudServer")
class MonadClusterServer {
  @XmlAttribute(name = "id")
  var id: Int = _
  @XmlAttribute(name = "address")
  var address: String = _
}

@XmlRootElement(name = "monad_cloud")
@XmlType(name = "MonadCloudConfig")
class MonadCloudConfig extends LogFileSupport {
  /** 自身ID设置 **/
  @XmlElement(name = "cloud_id")
  var myId: Int = _
  /** 数据存放的目录 **/
  @XmlElement(name = "data_dir")
  var dataDir: String = "data"
  /** timeouts ZooKeeper uses to limit the length of time the ZooKeeper servers in quorum have to connect to a leader. **/
  @XmlElement(name = "init_limit")
  var initLimit: Int = 5
  /** how far out of date a server can be from a leader **/
  @XmlElement(name = "sync_limit")
  var syncLimit: Int = 2
  /** 监听的端口 **/
  @XmlElement(name = "port")
  var port: Int = _
  /** 集群服务列表 TODO 能否自动化 **/
  @XmlElementWrapper(name = "servers")
  @XmlElement(name = "server")
  var servers = new util.ArrayList[MonadClusterServer]
}
