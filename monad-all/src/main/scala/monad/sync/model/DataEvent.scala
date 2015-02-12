// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.sync.model


/**
 * 数据事件
 * @author jcai
 */
class DataEvent {
  //资源名称
  var resourceName: String = null
  //数据
  var row: Array[Any] = null
  var timestamp: Long = -1
  var version: Int = 0

  def reset() {
    resourceName = null
    row = null
    version = 0
    timestamp = -1
  }
}
