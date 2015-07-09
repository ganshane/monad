// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.services

import monad.face.services.ResourceSearcher
import monad.jni.services.gen.SlaveNoSQLSupport
import monad.support.services.ServiceLifecycle
import org.apache.lucene.document.Document

/**
 * 资源索引的接口定义
 * @author jcai
 */
trait ResourceIndexer
  extends ServiceLifecycle {
  /**
   * 对文档进行索引
   * @param doc 文档对象
   */
  def indexDocument(doc: Document, dataVersion: Int)

  def updateDocument(id: Int, doc: Document, dataVersion: Int)

  def deleteDocument(id: Int, dataVersion: Int)

  /**
   * submit操作
   * @param logSeq 版本号
   */
  def commit(logSeq: Long, dataVersion: Int)

  /**
   * 得到资源搜索对象
   * @return 资源搜索对象
   */
  def getResourceSearcher: ResourceSearcher

  /**
   * 删除索引
   */
  def removeIndex()

  def findObject(key: Int): Option[Array[Byte]]

  def findObjectId(idSeq: Int): Option[Array[Byte]]

  def nosqlOpt(): Option[SlaveNoSQLSupport]
}
