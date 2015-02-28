// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import monad.core.MonadCoreSymbols
import monad.support.MonadSupportConstants
import monad.support.services.SymbolExpander

import scala.io.Source

/**
 * process config file
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-01-30
 */
object MonadConfigFileUtils {
  def findConfigPath(serverHome: String, configFile: String) = {
    var filePath = serverHome + "/config"
    try {
      filePath = SymbolExpander.valueForSymbol(MonadCoreSymbols.CONFIG_DIR)
    } catch {
      case e: Throwable => //ignore exception
    }
    filePath += "/" + configFile
    Source.fromFile(filePath, MonadSupportConstants.UTF8_ENCODING).mkString
  }
}
