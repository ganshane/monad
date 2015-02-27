// Copyright 2011,2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai. 
 * site: http://www.ganshane.com
 */

package monad.api

import com.google.protobuf.ExtensionRegistry
import monad.api.internal._
import monad.api.services._
import monad.core.services.ServiceLifecycleHub
import monad.face.MonadFaceConstants
import monad.face.config.ApiConfigSupport
import monad.face.model.{IdShardResultCollect, OpenBitSetWithNodes}
import monad.face.services.{ResourceDefinitionLoaderListener, RpcSearcherFacade}
import monad.protocol.internal.InternalMaxdocQueryProto
import monad.rpc.services.ProtobufExtensionRegistryConfiger
import monad.support.services.ServiceLifecycle
import org.apache.tapestry5.ioc._
import org.apache.tapestry5.ioc.annotations._
import org.apache.tapestry5.services.{ComponentEventResultProcessor, RequestFilter, RequestHandler}

/**
 * API Module
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.3
 */
object LocalMonadApiModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[DynamicTraceService], classOf[DynamicTraceServiceImpl]).withId("DynamicTraceService")
    binder.bind(classOf[RelationService], classOf[RelationServiceImpl]).withId("RelationService")
    binder.bind(classOf[SearcherQueueManagerImpl]).withId("SearcherQueueManagerImpl")
    binder.bind(classOf[SearchResultExtractor]).withId("SearchResultExtractor")
    binder.bind(classOf[ResourceStater]).withId("ResourceStater")
    binder.bind(classOf[SearcherFacade], classOf[SearcherFacadeImpl]).withId("SearcherFacade")
    binder.bind(classOf[ObjectIdCreator], classOf[ObjectIdCreatorImpl]).withId("ObjectIdCreator")
    binder.bind(classOf[ResourceRequest], classOf[ResourceRequestImpl]).
      scope(ScopeConstants.PERTHREAD).
      withId("ResourceRequest")
    binder.bind(classOf[RpcSearcherFacade], classOf[RemoteRpcSearcherFacade]).withId("RpcSearcherFacade")
  }

  def buildMemcachedClient(apiConfigSupport: ApiConfigSupport) = {
    new SpyMemcachedClient(apiConfigSupport.api.enableMemcachedCache,
      apiConfigSupport.api.memcachedServers,
      apiConfigSupport.api.expiredPeriodInMinutes)
  }

  @Scope(ScopeConstants.PERTHREAD)
  def buildSearcherQueue(resourceRequest: ResourceRequest, searcherManager: SearcherQueueManagerImpl): SearcherQueue = {
    searcherManager.getObject(resourceRequest.getResourceDefinition.name)
  }

  @Contribute(classOf[ResourceDefinitionLoaderListener])
  def provideResourceDefinitionLoaderListener(configuration: OrderedConfiguration[ResourceDefinitionLoaderListener],
                                              @Local searcherQueueManager: SearcherQueueManagerImpl) {
    configuration.add("api", searcherQueueManager)
  }

  @Contribute(classOf[RequestHandler])
  def provideResourceRequest(configuration: OrderedConfiguration[RequestFilter]) {
    configuration.addInstance("resourceRequest", classOf[ResourceRequestFilter])
  }

  @Contribute(classOf[ServiceLifecycleHub])
  def provideMemcached(configuration: OrderedConfiguration[ServiceLifecycle], memcache: MemcachedClient) {
    configuration.add("MemcachedClient", memcache, "after:" + MonadFaceConstants.LIFE_RESOURCES)
  }

  @Contribute(classOf[ComponentEventResultProcessor[_]])
  def provideOpenBitSetResultProcessor(configuration: MappedConfiguration[Class[_ <: AnyRef], ComponentEventResultProcessor[_ <: AnyRef]]) {
    configuration.addInstance(classOf[OpenBitSetWithNodes], classOf[OpenBitSetResultProcessor])
    configuration.addInstance(classOf[IdShardResultCollect], classOf[IdShardResultCollectResultProcessor])
  }

  @Contribute(classOf[ExtensionRegistry])
  def provideProtobufCommand(configuration: Configuration[ProtobufExtensionRegistryConfiger]) {
    configuration.add(new ProtobufExtensionRegistryConfiger {
      override def config(registry: ExtensionRegistry): Unit = {
        InternalMaxdocQueryProto.registerAllExtensions(registry)
      }
    })
  }
}
