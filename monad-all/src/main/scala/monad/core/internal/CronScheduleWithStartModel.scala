// Copyright 2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core.internal

import org.apache.tapestry5.ioc.services.cron.CronSchedule

/**
 * 支持启动模式
 * @author jcai
 */
//scheuler start mode
abstract class SchedulerStartMode

//start at once
case object StartAtOnce extends SchedulerStartMode

//start at delay
case object StartAtDelay extends SchedulerStartMode

class CronScheduleWithStartModel(cronExpression: String, startModel: SchedulerStartMode) extends CronSchedule(cronExpression) {
  override def firstExecution() = {
    startModel match {
      case StartAtDelay =>
        super.firstExecution()
      case StartAtOnce =>
        0L
    }
  }
}
