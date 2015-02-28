// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face

/**
 * API 常量
 * @author jcai
 */
object ApiConstants {
    //API 常量
    final val STATUS="status"
    final val SUCCESS="success"
    final val DATA="data"
    final val MSG="msg"

    //======== GROUP 提供API相关

    //获取所有资源，返回资源JSON字符串，key为资源名称，value为资源的xml定义
    val GROUP_GetSelfGroupConfig="GetSelfGroupConfig"
    val GROUP_GetOtherGroups="GetOtherGroups"
    val GROUP_GetResources="GetResources"
    val GROUP_GetRouterServer="GetRouterServer"
    val GROUP_GetCloudAddress="GetCloudAddress"
}