// Copyright 2012,2013,2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import org.apache.tapestry5.ioc.internal.services.{MapSymbolProvider, SymbolSourceImpl, SystemEnvSymbolProvider, SystemPropertiesSymbolProvider}

import scala.collection.JavaConversions._

/**
 * 提供解析表达式
 * @author jcai
 */
object SymbolExpander {

  /**
   * 解析字符串
   */
  def expand(input: String, params: Map[String, String] = Map[String, String]()) = {
    val symbolSource = new SymbolSourceImpl(List(
      new SystemPropertiesSymbolProvider,
      new SystemEnvSymbolProvider,
      new MapSymbolProvider(params)
    ))
    symbolSource.expandSymbols(input)
  }

  def valueForSymbol(symbolName: String) = {
    val symbolSource = new SymbolSourceImpl(List(
      new SystemPropertiesSymbolProvider,
      new SystemEnvSymbolProvider
    ))
    symbolSource.valueForSymbol(symbolName)
  }
}
