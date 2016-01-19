// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

/**
 * machine heart beat
 */
trait MachineHeartbeat {
  /**
   * 得到机器的ID
   * @return 机器ID信息
   */
  def findMachineId(): String
}
