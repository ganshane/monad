// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import org.apache.tapestry5.ioc.services.cron.CronSchedule

/**
 * cron schedule
 */
//scheuler start mode
sealed abstract class SchedulerStartMode

//start at once
case object StartAtOnce extends SchedulerStartMode

//start at delay
case object StartAtDelay extends SchedulerStartMode

class CronScheduleWithStartModel(cronExpression: String, startModel: SchedulerStartMode)
  extends CronSchedule(cronExpression) {
  override def firstExecution() = {
    startModel match {
      case StartAtDelay =>
        super.firstExecution()
      case StartAtOnce =>
        System.currentTimeMillis()
    }
  }
}
