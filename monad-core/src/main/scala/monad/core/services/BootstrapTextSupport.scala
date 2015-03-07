// Copyright 2012,2013,2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services

import java.io.{BufferedInputStream, IOException}
import java.util.Properties

import org.fusesource.jansi.Ansi
import org.slf4j.Logger

/**
 * 输出版本
 * @author jcai
 */
trait BootstrapTextSupport {
  def readClientVersionNumber(resourcePath: String): String = {
    var result = "UNKNOWN"

    var stream = Thread.currentThread().getContextClassLoader.getResourceAsStream(
      resourcePath)


    if (stream != null) {
      val properties = new Properties()


      try {
        stream = new BufferedInputStream(stream)

        properties.load(stream)
      }
      catch {
        case ex: IOException =>
        // Just ignore it.
      }

      val version = properties.getProperty("version")
      val buildNumber = properties.getProperty("buildNumber")
      val buildId = properties.getProperty("buildId")

      if (version != null) result = version
      if (buildNumber != null && !buildNumber.contains("$")) result += "_" + buildNumber
      if (buildId != null && !buildId.contains("$")) result += "_" + buildId
    }

    result

  }

  def readVersionNumber(resourcePath: String): String = {
    var result = "UNKNOWN"

    var stream = Thread.currentThread().getContextClassLoader.getResourceAsStream(
      resourcePath)


    if (stream != null) {
      val properties = new Properties()


      try {
        stream = new BufferedInputStream(stream)

        properties.load(stream)
      }
      catch {
        case ex: IOException =>
        // Just ignore it.
      }

      val version = properties.getProperty("version")
      val buildNumber = properties.getProperty("buildNumber")
      val buildId = properties.getProperty("buildId")

      if (version != null) result = version
      if (buildNumber != null) result += "#" + buildNumber
      if (buildId != null) result += "@" + buildId

    }

    result
  }

  protected[monad] def printTextWithNative(logger: Logger,
                                           logo: String,
                                           values: Any*) {
    /*
    //val version = readVersionNumber(versionPath)
    var str = """
   _  _________ _   _____   _  _____
  / |/ /  _/ _ \ | / / _ | / |/ / _ | module : %s
 /    // // , _/ |/ / __ |/    / __ | version: %s
/_/|_/___/_/|_||___/_/ |_/_/|_/_/ |_| native : %s

              """
     */
    var str = logo
    try {
      //AnsiConsole.systemInstall();

      str = Ansi.ansi().eraseScreen().render(logo, values.map(_.asInstanceOf[Object]): _ *).toString
      /*
      val className = "org.fusesource.jansi.Ansi"
      val clazz = Thread.currentThread().getContextClassLoader.loadClass(className)
      //ansi()
      val obj = clazz.getMethod("ansi").invoke(null)
      //ansi().render
      str = clazz.
        getMethod("render", classOf[String]).
        invoke(obj, "@|green " + str.
        format("|@ @|red " + text + "|@ @|green ", "|@ @|yellow " + version + "|@ @|green ", "|@ @|cyan " + nativeVersion + "|@")).toString
      */
      logger.info(str)
    } finally {
      //AnsiConsole.systemUninstall()
    }
  }
}
