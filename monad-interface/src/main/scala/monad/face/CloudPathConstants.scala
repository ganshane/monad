// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */

package monad.face

/**
 * 云平台路径的常量
 * @author jcai
 */
object CloudPathConstants {
    //某一group下的子路径, 譬如：/groups/sc
    final val RESOURCES_PATH="/resources"
    final val RESOURCE_PATH_FORMAT=RESOURCES_PATH+"/%s"
    //记录增量字段的path
    final val RESOURCE_MAX_PATH_FORMAT=RESOURCE_PATH_FORMAT+"/max"
    //资源的id序列记录
    final val RESOURCE_ID_PATH_FORMAT=RESOURCE_PATH_FORMAT+"/seq"
    //记录总数量的path
    final val RESOURCE_TOTAL_PATH_FORMAT=RESOURCE_PATH_FORMAT+"/total"
    final val RESOURCE_REGION_INFO_PATH_FORMAT=RESOURCE_PATH_FORMAT+"/info"
    final val RESOURCE_RESYNC_PATH_FORMAT=RESOURCE_PATH_FORMAT+"/resync"
    final val RESOURCE_SEARCHER_PATH_FORMAT=RESOURCES_PATH+"/%s/searcher"
  
  final val NODE_PATH_FORMAT="/nodes"
  final val DYNAMIC_PATH="/dynamic"
  final val RELATION_PATH="/relation"
}
