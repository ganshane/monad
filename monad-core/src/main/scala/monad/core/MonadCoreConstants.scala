// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core

/**
 * constants
 */
object MonadCoreConstants {

  //根路径，变为可配置？
  // 不在使用，使用 ZkConfig.root
  final val ROOT_PATH = "/monad"
  final val GROUPS_PATH="/groups"
  final val LIVE_PATH="/live"

  //------- 以下均为子目录设置
  //机器注册的地址，尤其是rpc的地址
  final val MACHINES = "/machines"
  //机器的心跳信息路径
  final val HEARTBEATS = "/heartbeats"
  //每台机器错误信息
  final val ERRORS = "/errors"


  //配置JMX的输出支持
  final val METRICS_REPORT_KEY="metrics.report"

}
