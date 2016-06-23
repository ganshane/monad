// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id.internal

import monad.core.config.ZkClientConfigSupport
import stark.utils.services.ZookeeperTemplate
import org.apache.tapestry5.ioc.annotations.EagerLoad

/**
 * id zk template
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
@EagerLoad
class IdZookeeperTemplate(config:ZkClientConfigSupport)
  extends ZookeeperTemplate(config.zk.address,None,config.zk.timeoutInMills){
}
