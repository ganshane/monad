// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import java.util.concurrent.TimeUnit

import com.codahale.metrics._
import monad.core.services.MetricsService
import monad.support.services.{LoggerSupport, RunInNoExceptionThrown}
import org.apache.tapestry5.ioc.services.cron.{CronSchedule, PeriodicExecutor}
import org.slf4j.LoggerFactory

/**
 * implements metrics service
 */
class MetricsServiceImpl(periodicExecutor: PeriodicExecutor)
  extends MetricsService
  with LoggerSupport
  with RunInNoExceptionThrown {
  private final val metrics: MetricRegistry = new MetricRegistry
  /*
  registerAll("jvm-thread-state", new ThreadStatesGaugeSet)
  registerAll("jvm-mem", new MemoryUsageGaugeSet)
  registerAll("jvm-gc", new GarbageCollectorMetricSet)
  */

  def unregister(name: String) {
    metrics.remove(name)
  }

  def register[T <: Metric](name: String, metric: T): T = {
    info("Register Metric " + name)
    metrics.register(name, metric)
  }

  def register(name: String, histogram: Histogram): Histogram = {
    info("Register Histogram " + name)
    metrics.histogram(name)
  }

  def registerCounter(name: String): Counter = {
    info("Register Metric " + name)
    metrics.counter(name)
  }

  override def registerTimer(name: String): Timer = {
    info("Register Metric timer " + name)
    metrics.timer(name)
  }

  def registerMeter(name: String): Meter = {
    info("Register Metric Meter " + name)
    metrics.meter(name)
  }

  def registerAll(prefix: String, metrics: MetricSet) {
    import scala.collection.JavaConversions._
    for (entry <- metrics.getMetrics.entrySet) {
      entry.getValue match {
        case metricSet: MetricSet =>
          registerAll(MetricRegistry.name(prefix, entry.getKey), metricSet)
        case _ =>
          register(MetricRegistry.name(prefix, entry.getKey), entry.getValue)
      }
    }
  }

  /**
   * 服务关闭
   */
  override def shutdown(): Unit = {
  }

  /**
   * 启动服务
   */
  override def start(): Unit = {
    /*
    val jolokiaConfig = new JvmAgentConfig("")
    val jolokia = new JolokiaServer(jolokiaConfig,false)
    jolokia.start()
    val jolokiaServer = JolokiaMBeanServerUtil.getJolokiaMBeanServer();

    val jmxReporter = JmxReporter.forRegistry(metrics)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .convertRatesTo(TimeUnit.SECONDS)
      .registerWith(jolokiaServer)
      .build()
    jmxReporter.start()
    */

    val reporter = Slf4jReporter.forRegistry(metrics)
      .outputTo(LoggerFactory.getLogger(classOf[MetricsServiceImpl]))
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build()
    //FIXES #100 手工关闭reporter的线程
    runInNotExceptionThrown {
      reporter.stop()
    }
    periodicExecutor.addJob(new CronSchedule("0 * * * * ? *"), "metrics-reporter", new Runnable {
      override def run(): Unit = {
        try {
          reporter.report()
        } catch {
          case e: Throwable =>
            error("fail to report metrics", e)
        }
      }
    })
  }
}
