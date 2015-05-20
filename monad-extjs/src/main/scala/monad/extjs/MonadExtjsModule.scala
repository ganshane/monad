// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs

import com.google.gson.{JsonArray, JsonObject}
import monad.extjs.internal._
import monad.extjs.model.ExtStreamResponse
import monad.extjs.services._
import org.apache.tapestry5.SymbolConstants
import org.apache.tapestry5.internal.services.{AssetResourceLocator, ResourceStreamer}
import org.apache.tapestry5.ioc._
import org.apache.tapestry5.ioc.annotations.{Contribute, Decorate, Local, Symbol}
import org.apache.tapestry5.ioc.services.{CoercionTuple, PropertyShadowBuilder, TypeCoercer}
import org.apache.tapestry5.services._
import org.apache.tapestry5.services.assets.{AssetRequestHandler, ContentTypeAnalyzer}
import org.apache.tapestry5.services.javascript.{JavaScriptStack, JavaScriptStackSource}
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2

/**
 * integrate tapestry5 and extjs4 module
 * @author jcai
 */
object MonadExtjsModule {
  def contributeAssetDispatcher(configuration: MappedConfiguration[String, AssetRequestHandler],
                                streamer: ResourceStreamer,
                                assetResourceLocator: AssetResourceLocator) {
    configuration.`override`("assets", new CoffeeScriptAssetRequestHandler(streamer, assetResourceLocator, "assets"))
  }

  def contributeClasspathAssetAliasManager(configuration: MappedConfiguration[String, String]) {
    configuration.add("assets", "assets")
  }
    def bind(binder:ServiceBinder){
        binder.bind(classOf[FileSystemAssetAliasManager], classOf[FileSystemAssetAliasManagerImpl]).withId("FileSystemAssetAliasManager")
        binder.bind(classOf[ExtDirectApiResolver],classOf[ExtDirectApiResolverImpl]).withId("ExtDirectApiResolver")
        binder.bind(classOf[ExtDirectRequestDecoder],classOf[ExtDirectRequestDecoderImpl]).withId("ExtDirectRequestDecoder")
        binder.bind(classOf[ExtRequestGlobals],classOf[ExtRequestGlobalsImpl]).withId("ExtRequestGlobals")
        binder.bind(classOf[AssetFactory], classOf[FileSystemAssetFactory]).withId("FileSystemAssetFactory")
    }
    //config extjs directory
    def contributeFactoryDefaults(configuration:MappedConfiguration[String, Object]){
        configuration.add(MonadExtjsConstants.EXT_JS_DIR,"/Users/jcai/DevLibs/ext-4.1.0-beta-1")
        configuration.add(MonadExtjsConstants.EXT_JS_PATH,"file://${"+MonadExtjsConstants.EXT_JS_DIR+"}")
    }
    @Contribute(classOf[ContentTypeAnalyzer])
    def setupContentTypeMapping( configuration:MappedConfiguration[String, String]){
        configuration.add("coffee","text/javascript")
    }
    //contribute extjs_static to FileSystemAssetAliasManager
    @Contribute(classOf[FileSystemAssetAliasManager])
    def provideExtjLibrary(configuration:MappedConfiguration[String, String],@Symbol(MonadExtjsConstants.EXT_JS_DIR) path:String){
        configuration.add("extjs_static",path)
    }

    //decorate ResourceStreamer,provide http 304 status to cache large javascript
    @Decorate(serviceInterface=classOf[ResourceStreamer])
    def provideCacheControl(@Symbol(SymbolConstants.PRODUCTION_MODE)productionMode:Boolean,
                            request:Request,response:Response, resourceStreamer:ResourceStreamer):ResourceStreamer={
        new CacheResourceStreamerImpl(productionMode,request,response,resourceStreamer)
    }

    //provide FileSystemAssetFactory asset factory
    @Contribute(classOf[AssetSource])
    def provideFileSystemAssetFactory(configuration:MappedConfiguration[String, AssetFactory],
                                      @Local fileAssetFactory:AssetFactory){
        configuration.add("file",fileAssetFactory)
    }

    //dispatch file system asset
    def contributeAssetDispatcher(configuration:MappedConfiguration[String, AssetRequestHandler],
                                  fileSystemAssetAliasManager:FileSystemAssetAliasManager,
                                  streamer:ResourceStreamer ,
                                  assetResourceLocator:AssetResourceLocator){
        fileSystemAssetAliasManager.getMappings.foreach(e=>{
            configuration.add(e._1, new FileSystemStreamHandler(streamer, assetResourceLocator, e._2))
        })
    }
    //provide extjs component and pages
    def contributeComponentClassResolver(configuration:Configuration[LibraryMapping])
    {
        configuration.add(new LibraryMapping(MonadExtjsConstants.EXTJS_FLAG, "monad.extjs"))
    }
    //provide extjs javascript stack
    @Contribute(classOf[JavaScriptStackSource])
    def provideExtjs(configuration:MappedConfiguration[String,JavaScriptStack]){
        configuration.addInstance(MonadExtjsConstants.EXTJS_FLAG,classOf[ExtJavaScriptStack])
    }
    //decorate request exception handler,add extjs exception reporter
    @Decorate(serviceInterface = classOf[RequestExceptionHandler])
    def provideExtRequestExcpetionHandler(delegate:RequestExceptionHandler,objectLocator:ObjectLocator):RequestExceptionHandler={
        val handler = objectLocator.autobuild(classOf[ExtRequestExceptionHandler])
        handler.setDelegate(delegate)

        handler
    }
    //add extjs event dispatcher
    def contributeMasterDispatcher(configuration:OrderedConfiguration[Dispatcher]){
        configuration.addInstance("ExtEvent",classOf[ExtEventDispatcher],"before:ComponentEvent","after:Asset")
    }
    //transform ExtDirectMethod annotation
    @Contribute(classOf[ComponentClassTransformWorker2])
    def provideTransformWorkers(configuration:OrderedConfiguration[ComponentClassTransformWorker2]){
        configuration.addInstance("ExtDirectMethod",classOf[ExtDirectMethodWorker])
    }
    //provide ExtStreamResponse ComponentEventResultProcessor
    @Contribute(classOf[ComponentEventResultProcessor[_]])
    def provideComponentEventResultProcessor(
                                              configuration:MappedConfiguration[Class[_ <: AnyRef], ComponentEventResultProcessor[_ <: AnyRef]]){
        configuration.addInstance(classOf[ExtStreamResponse],classOf[ExtStreamResponseResultProcessor])
    }
    //contribute HttpServletRequestHandler
    @Contribute(classOf[HttpServletRequestHandler])
    def provideHttpServletRequestHandler(configuration:OrderedConfiguration[HttpServletRequestFilter]){
        configuration.addInstance("ExtFilter",classOf[ExtFilter])
    }
    //build ext request
    def buildExtRequest(shadowBuilder:PropertyShadowBuilder,extRequestGlobals:ExtRequestGlobals):ExtRequest={
        shadowBuilder.build(extRequestGlobals, "extRequest", classOf[ExtRequest])
    }
    //provide String-> JsonObject and String -> JsonArray
    @Contribute(classOf[TypeCoercer])
    def provideCoercions(configuration:Configuration[CoercionTuple[_,_]])
    {
        configuration.add(CoercionTuple.create(classOf[String], classOf[JsonObject], new StringToJsonObject()))
        configuration.add(CoercionTuple.create(classOf[String], classOf[JsonArray], new StringToJsonArray()))
    }
}
