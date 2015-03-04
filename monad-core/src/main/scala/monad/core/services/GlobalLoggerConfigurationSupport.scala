// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import java.util.Properties
import java.util.logging.LogManager

import org.apache.log4j.PropertyConfigurator
import org.slf4j.bridge.SLF4JBridgeHandler

/**
 * global log4j configuration
 */
trait GlobalLoggerConfigurationSupport {
  protected def configLogger(logFile: String, prefix: String, loggerPrefix: Option[Array[String]] = None) {
    //convert jcl to slf4j
    val rootLogger = LogManager.getLogManager.getLogger("")
    val handlers = rootLogger.getHandlers
    for (handler <- handlers) {
      rootLogger.removeHandler(handler)
    }
    SLF4JBridgeHandler.install()

    //spymemcached logger
    System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SLF4JLogger")

    //debug mode or enable log
    if (System.getProperty("enable-log") != "true") {
      System.setProperty("log4j.defaultInitOverride", "false")
      val properties = new Properties()
      properties.put("log4j.rootCategory", "error,R")
      properties.put("log4j.appender.R", "org.apache.log4j.RollingFileAppender")
      properties.put("log4j.appender.R.layout", "org.apache.log4j.PatternLayout")
      properties.put("log4j.appender.R.layout.ConversionPattern", "[" + prefix + "] %d{MM-dd HH:mm:ss} %m%n")
      properties.put("log4j.appender.R.File", logFile)
      properties.put("log4j.appender.R.MaxFileSize", "10000KB")
      properties.put("log4j.appender.R.MaxBackupIndex", "10")
      loggerPrefix foreach { x => x.foreach(p => properties.put("log4j.category." + p, "info"))}
      properties.put("log4j.category.org.apache.zookeeper", "warn")
      properties.put("log4j.category.com.netflix", "warn")

      PropertyConfigurator.configure(properties)
    }
  }
}
