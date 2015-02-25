// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.api.services

import monad.face.model.ResourceRelation

/**
 * 关联关系定义的查询
 * @author jcai
 */
trait RelationService {
  /**
   * 通过关系ID获取某一关系定义
   * @param relationId 关系标示
   */
  def findRelation(relationId: String): Option[ResourceRelation.Rel]

  /**
   * 得到所有的关系定义
   * @return 所有的关系定义
   */
  def findRelations: Iterator[ResourceRelation.Rel]

  /**
   * 得到资源列表
   * @return 资源列表
   */
  def getResources: List[String]
}
