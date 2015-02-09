// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * web server configuration support
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WebServerConfigSupport")
trait WebServerConfigSupport {
  @XmlElement(name = "web")
  var web = new WebServerConfig
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WebServerConfig")
class WebServerConfig {
  @XmlElement(name = "bind")
  var bind: String = _
  @XmlElement(name = "min_conn_count")
  var minConnCount = 5
  @XmlElement(name = "max_conn_count")
  var maxConnCount = 50
  @XmlElement(name = "keep_alive_time_minutes")
  var keepAliveTimeInMinutes = 1
  @XmlElement(name = "waiting_queue_size")
  var waitingQueueSize = 1000
  @XmlElement(name = "socket_acceptor")
  var acceptor = 0
  @XmlElement(name = "socket_backlog")
  var backlog = 50
  @XmlElement(name = "conn_idle_time_secs")
  var idleTimeSecs = 3
  @XmlElement(name = "request_buffer_size_kb")
  var requestBufferSizeKB = 30
  @XmlElement(name = "response_buffer_size_kb")
  var responseBufferSizeKB = 30
}
