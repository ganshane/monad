// Copyright 2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import monad.face.model.{IdShardResult, ShardResult}

/**
 * nosql 的调用
 * @author jcai
 */
trait IdFacade {
  /**
   * 通过服务器的ID和资源名称，以及id序列，来查找对象的ID值
   * @return id的值
   */
  def findObjectId(category:String,ids:Array[Int]):Array[String]
  def batchAddId(category:String,labels:Array[String]):Array[Int]
  def putIfAbsent(category:String,label:String):Int
}

trait RpcSearcherFacade {
  /**
   * search index with index name and keyword
   */
  def collectSearch(resourceName: String, q: String, sort: String, topN: Int): ShardResult

  def facetSearch(resourceName: String, q: String, field: String, upper: Int, lower: Int): ShardResult

  def maxDoc(resourceName: String): Long

  /**
   * 搜索对象
   * @param resourceName 资源名称
   * @param q 搜索条件
   * @return 搜索比中结果
   */
  def searchObjectId(resourceName:String,q:String):IdShardResult

  /**
   * 查找对象的详细信息
   * @param serverId 服务器的Hash值
   * @param resourceName 资源名称
   * @param key 键值
   * @return 数据值
   */
  def findObject(serverId: Short, resourceName: String, key: Int): Option[Array[Byte]]

}
