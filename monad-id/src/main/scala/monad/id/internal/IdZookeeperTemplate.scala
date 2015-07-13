package monad.id.internal

import monad.core.config.ZkClientConfigSupport
import monad.support.services.ZookeeperTemplate
import org.apache.tapestry5.ioc.annotations.EagerLoad

/**
 * id zk template
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
@EagerLoad
class IdZookeeperTemplate(config:ZkClientConfigSupport)
  extends ZookeeperTemplate(config.zk.address,Some(config.zk.root),config.zk.timeoutInMills){
}
