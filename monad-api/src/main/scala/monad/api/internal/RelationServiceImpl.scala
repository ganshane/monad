// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.api.internal

import monad.api.services.RelationService
import monad.face.CloudPathConstants
import monad.face.model.ResourceRelation
import monad.support.MonadSupportConstants
import monad.support.services.{NodeDataWatcher, XmlLoader, ZookeeperTemplate}

import scala.collection.JavaConversions._

/**
 * 实现关系服务
 * @author jcai
 */
class RelationServiceImpl(zookeeper: ZookeeperTemplate) extends RelationService {
  private var rr: ResourceRelation = _
  private var relations = Map[String, ResourceRelation.Rel]()
  zookeeper.watchNodeData(CloudPathConstants.RELATION_PATH, new NodeDataWatcher {
    def handleNodeDeleted() {
      relations = Map[String, ResourceRelation.Rel]()
    }

    def handleDataChanged(data: Option[Array[Byte]]) {
      reloadRelation(data)
    }
  })

  def findRelation(relationId: String) = relations.get(relationId)

  def findRelations = relations.values.iterator

  def getResources = {
    val groupResourcesPath = "/resources"
    zookeeper.
      getChildren(groupResourcesPath).
      map(x => zookeeper.getDataAsString(groupResourcesPath + "/" + x)).
      filter(_.isDefined).map(_.get).toList
  }

  private def reloadRelation(data: Option[Array[Byte]]) {
    data match {
      case None =>
        relations = Map[String, ResourceRelation.Rel]()
      case Some(str) =>
        rr = XmlLoader.parseXML[ResourceRelation](new String(str, MonadSupportConstants.UTF8_ENCODING))
        relations = rr.relations.foldLeft(Map[String, ResourceRelation.Rel]()) { (m, x) =>
          m + (x.name -> x)
        }
    }
  }
}
