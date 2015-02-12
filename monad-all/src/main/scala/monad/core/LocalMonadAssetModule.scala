// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.core

import monad.core.internal.JsonApiResponseResultProcessor
import monad.core.model.JsonApiResponse
import monad.extjs.internal.CoffeeScriptAssetRequestHandler
import org.apache.tapestry5.SymbolConstants
import org.apache.tapestry5.internal.services.{AssetResourceLocator, ResourceStreamer}
import org.apache.tapestry5.ioc.MappedConfiguration
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.services.ComponentEventResultProcessor
import org.apache.tapestry5.services.assets.AssetRequestHandler

/**
 * Web模块使用到模块
 * @author jcai
 */
object LocalMonadAssetModule {
  @Contribute(classOf[ComponentEventResultProcessor[_]])
  def contributeComponentEventResultProcessor(configuration: MappedConfiguration[Class[_], ComponentEventResultProcessor[_]]) {
    configuration.addInstance(classOf[JsonApiResponse], classOf[JsonApiResponseResultProcessor])
  }

  def contributeAssetDispatcher(configuration: MappedConfiguration[String, AssetRequestHandler],
                                streamer: ResourceStreamer,
                                assetResourceLocator: AssetResourceLocator) {
    configuration.`override`("assets", new CoffeeScriptAssetRequestHandler(streamer, assetResourceLocator, "assets"))
  }

  def contributeClasspathAssetAliasManager(configuration: MappedConfiguration[String, String]) {
    configuration.add("assets", "assets")
  }

  def contributeApplicationDefaults(configuration: MappedConfiguration[String, Object]) {
    //configuration.add(SymbolConstants.APPLICATION_VERSION,ContainerUtil.readClientVersionNumber("META-INF/maven/com.ganshane.monad/monad-all/version.properties"))
    configuration.add(SymbolConstants.OMIT_GENERATOR_META, java.lang.Boolean.TRUE)
  }
}
