// Copyright 2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import java.util.concurrent.ExecutorService

import monad.face.model.{IdShardResult, ResourceDefinition, ShardResult}
import stark.utils.services.ServiceLifecycle
import org.apache.lucene.index.IndexWriter

/**
 * 搜索的plugin
 * @author jcai
 */
trait ResourceSearcherSupport {
    /**
     * search index with index name and keyword
     */
    def collectSearch(q:String,sort:String,topN:Int):ShardResult
    def facetSearch(q:String,field:String,upper:Int, lower:Int):ShardResult
    def maxDoc:Int
    def searchObjectId(q:String):IdShardResult
}
trait ResourceSearcher extends ResourceSearcherSupport with ServiceLifecycle{
    def maybeRefresh()
}
trait ResourceSearcherSource{
    def newResourceSearcher(rd:ResourceDefinition,indexWriter:IndexWriter,regionId:Short,executor:ExecutorService):ResourceSearcher
}
trait ResourceSearcherFactory{
    def createSearcher(rd:ResourceDefinition,indexWriter:IndexWriter,regionId:Short,executor:ExecutorService):ResourceSearcher
}

