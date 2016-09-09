// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai. 
 * site: http://www.ganshane.com
 */

package monad.api

import com.google.protobuf.ExtensionRegistry
import monad.api.internal._
import monad.api.services._
import monad.core.MonadCoreConstants
import monad.face.config.ApiConfigSupport
import monad.face.internal.RemoteIdFacade
import monad.face.model.{IdShardResultCollect, OpenBitSetWithNodes}
import monad.face.services.{GroupServerApi, IdFacade, ResourceDefinitionLoaderListener, RpcSearcherFacade}
import monad.protocol.internal.{InternalFindDocProto, InternalIdProto, InternalMaxdocQueryProto, InternalSearchProto}
import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants}
import org.apache.tapestry5.ioc._
import org.apache.tapestry5.ioc.annotations._
import org.apache.tapestry5.services.{ComponentEventResultProcessor, RequestFilter, RequestHandler}
import roar.api.services.RoarClient
import stark.rpc.services.ProtobufExtensionRegistryConfiger

/**
 * API Module
  *
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
    binder.bind(classOf[IdFacade], classOf[RemoteIdFacade]).withId("RemoteIdFacade")
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

  @Contribute(classOf[ComponentEventResultProcessor[_]])
  def provideOpenBitSetResultProcessor(configuration: MappedConfiguration[Class[_ <: AnyRef], ComponentEventResultProcessor[_ <: AnyRef]]) {
    configuration.addInstance(classOf[OpenBitSetWithNodes], classOf[OpenBitSetResultProcessor])
    configuration.addInstance(classOf[IdShardResultCollect], classOf[IdShardResultResultProcessor])
  }

  @Contribute(classOf[ExtensionRegistry])
  def provideProtobufCommand(configuration: Configuration[ProtobufExtensionRegistryConfiger]) {
    configuration.add(new ProtobufExtensionRegistryConfiger {
      override def config(registry: ExtensionRegistry): Unit = {
        InternalMaxdocQueryProto.registerAllExtensions(registry)
        InternalSearchProto.registerAllExtensions(registry)
        InternalFindDocProto.registerAllExtensions(registry)
        InternalIdProto.registerAllExtensions(registry)
      }
    })
  }
  def buildRoarClient(groupApi:GroupServerApi):RoarClient={
    val conf = HBaseConfiguration.create()
    conf.set(HConstants.ZOOKEEPER_QUORUM, groupApi.GetCloudAddress)
    conf.set(HConstants.HBASE_CLIENT_RETRIES_NUMBER,"3")
    conf.set(HConstants.HBASE_RPC_TIMEOUT_KEY,"30000")
    //  conf.set(HConstants.HBASE_CLIENT_IPC_POOL_SIZE)
    conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT,MonadCoreConstants.GROUPS_PATH + "/" + groupApi.GetSelfGroupConfig.id)

    new RoarClient(conf)
  }
}
