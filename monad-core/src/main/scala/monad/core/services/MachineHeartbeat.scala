// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import monad.support.services.ServiceLifecycle

/**
 * machine heart beat
 */
trait MachineHeartbeat extends ServiceLifecycle {
  /**
   * 得到机器的ID
   * @return 机器ID信息
   */
  def findMachineId(): String
}
