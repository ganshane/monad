// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import monad.core.MonadCoreSymbols
import stark.utils.StarkUtilsConstants
import stark.utils.services.SymbolExpander

import scala.io.Source
import scala.util.control.NonFatal

/**
 * process config file
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-01-30
 */
object MonadConfigFileUtils {
  def readConfigFileContent(serverHome: String, configFile: String) = {
    var filePath = serverHome + "/config"
    try {
      filePath = SymbolExpander.valueForSymbol(MonadCoreSymbols.CONFIG_DIR)
    } catch {
      case NonFatal(e) => //ignore exception
    }
    filePath += "/" + configFile
    Source.fromFile(filePath, StarkUtilsConstants.UTF8_ENCODING).mkString
  }
}
