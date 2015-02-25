// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.api.internal

import java.util.concurrent.CountDownLatch

import monad.api.services.{DynamicTraceService, MonadApiExceptionCode}
import monad.face.CloudPathConstants
import monad.face.model.DynamicResourceDefinition
import monad.face.services.ResourceDefinitionLoader
import monad.support.MonadSupportConstants
import monad.support.services.{MonadException, NodeDataWatcher, XmlLoader, ZookeeperTemplate}
import org.slf4j.LoggerFactory

/**
 * 实现动态轨迹的服务
 * @author jcai
 */
class DynamicTraceServiceImpl(zookeeper: ZookeeperTemplate,
                              resourceDefinitionLoader: ResourceDefinitionLoader)
  extends DynamicTraceService {
  private val logger = LoggerFactory getLogger getClass
  private val countLatch = new CountDownLatch(1)
  private var drd: Option[DynamicResourceDefinition] = None
  //监控 dynamic_path
  zookeeper.watchNodeData(CloudPathConstants.DYNAMIC_PATH,
    new NodeDataWatcher {
      def handleNodeDeleted() {
        logger.debug("dynamic resource definition is None")
        drd = None
      }

      def handleDataChanged(data: Option[Array[Byte]]) {
        reloadDynamic(data)
      }
    })
  countLatch.await()

  def getDynamicResource: Iterator[String] = {
    logger.debug("dynamic resource definition is {}", drd)
    drd match {
      case Some(x) =>
        resourceDefinitionLoader.getResourceDefinitions.
          filter(_.dynamic).
          map(_.name)
      case None =>
        Nil.iterator
    }
  }

  def getDynamicResourceDefinition: DynamicResourceDefinition = {
    drd match {
      case Some(x) => x
      case None => throw new MonadException("未能发现动态资源定义为空", MonadApiExceptionCode.FAIL_FIND_DYNAMIC_DEFINITION)
    }
  }

  private def reloadDynamic(data: Option[Array[Byte]]) {
    data match {
      case Some(x) =>
        logger.debug("loading dynamic resource definition")
        drd = Some(XmlLoader.parseXML[DynamicResourceDefinition](new String(x, MonadSupportConstants.UTF8_ENCODING)))
        logger.debug("parse dynamic resource definition is {}", drd)
      case None =>
        logger.debug("dynamic resource definition is None")
        drd = None
    }
    countLatch.countDown()
  }
}
