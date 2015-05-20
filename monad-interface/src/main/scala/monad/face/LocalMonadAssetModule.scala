// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face

import monad.face.internal.JsonApiResponseResultProcessor
import monad.face.model.JsonApiResponse
import org.apache.tapestry5.SymbolConstants
import org.apache.tapestry5.ioc.MappedConfiguration
import org.apache.tapestry5.ioc.annotations.Contribute
import org.apache.tapestry5.services.ComponentEventResultProcessor

/**
 * Web模块使用到模块
 * @author jcai
 */
object LocalMonadAssetModule {
  @Contribute(classOf[ComponentEventResultProcessor[_]])
  def contributeComponentEventResultProcessor(configuration: MappedConfiguration[Class[_], ComponentEventResultProcessor[_]]) {
    configuration.addInstance(classOf[JsonApiResponse], classOf[JsonApiResponseResultProcessor])
  }

  def contributeApplicationDefaults(configuration: MappedConfiguration[String, Object]) {
    //configuration.add(SymbolConstants.APPLICATION_VERSION,ContainerUtil.readClientVersionNumber("META-INF/maven/com.ganshane.monad/monad-all/version.properties"))
    configuration.add(SymbolConstants.OMIT_GENERATOR_META, java.lang.Boolean.TRUE)
  }
}
