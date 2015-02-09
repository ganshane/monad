// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.util.concurrent.{CopyOnWriteArrayList, CopyOnWriteArraySet}

import monad.support.MonadSupportConstants
import org.apache.zookeeper.CreateMode

import scala.collection.JavaConversions._

/**
 * ephemeral path support
 */
trait ZkEphemeralPathSupport {
  this: ZkClientSupport
    with ZkDeletePathSupport
    with LoggerSupport
    with ZkPathCreatorSupport =>

  /**
   * 记录所有的临时节点,以及异常的临时节点
   * 临时节点被删除，则watch将失效,
   * 但是当临时节点重新被创建，则此watch又重新生效
   */
  private final val ephemeralNodes = new CopyOnWriteArraySet[Node]()
  private final val failedEphemeralNodes = new CopyOnWriteArrayList[Node]()

  /**
   * 创建临时节点
   * @param path 目标路径
   * @param data 原始数据
   * @param mode 模式
   * @param isPermanent 永远跟踪
   */
  def createEphemeralPathWithStringData(path: String,
                                        data: Option[String] = None,
                                        mode: EphemeralMode = NormalEphemeralMode,
                                        isPermanent: Boolean = true) {
    data match {
      case Some(str) =>
        createEphemeralPath(path,
          Some(str.getBytes(MonadSupportConstants.UTF8_ENCODING)),
          mode,
          isPermanent)
      case None =>
        createEphemeralPath(path,
          None,
          mode,
          isPermanent)
    }
  }

  /**
   * 创建临时节点
   * @param path 目标路径
   * @param data 原始数据
   * @param mode 模式
   * @param isPermanent 永远跟踪
   */
  def createEphemeralPath(path: String,
                          data: Option[Array[Byte]] = None,
                          mode: EphemeralMode = NormalEphemeralMode,
                          isPermanent: Boolean = true) {
    mode match {
      case NormalEphemeralMode =>
        createEphemeralPath(path, data, CreateMode.EPHEMERAL, isPermanent)
      case SequenceEphemeralMode =>
        createEphemeralPath(path, data, CreateMode.EPHEMERAL_SEQUENTIAL, isPermanent)
      case other =>
        error("mode " + other + " unsupported!")
    }
  }

  private def createEphemeralPath(path: String,
                                  data: Option[Array[Byte]],
                                  mode: CreateMode,
                                  isPermanent: Boolean) {
    /**
     * 此处有一个特别的处理，因为在创建临时节点的时候，
     * 有可能是上一个session未关闭造成此临时节点仍然存在，
     * 如果只是简单setData，那么当上一个session失效的时候，本临时节点将消失
     *
     * @see http://developers.blog.box.com/2012/04/10/a-gotcha-when-using-zookeeper-ephemeral-nodes/
     */
    try {
      delete(path)
    } catch {
      case e: Throwable =>
        warn("fail to delete path when create ephemeral path for " + path)
    }
    val node = new Node(path, data, mode)
    try {
      failedEphemeralNodes.remove(path)
      internalCreatePath(path, data, node.createMode)
      if (isPermanent) {
        ephemeralNodes.add(node)
      }
    } catch {
      case e: Throwable =>
        warn(e.getMessage + ",will retry")
        failedEphemeralNodes.add(node)
    }
  }

  protected def retryFailedEphemeralNodes() {
    info("retry to create ephemeral nodes")
    val it = failedEphemeralNodes.iterator()
    while (it.hasNext) {
      val node = it.next()
      createEphemeralPath(node.path, node.data, node.createMode, isPermanent = true)
    }
  }

  protected def recreateEphemeralNodes() {
    //针对临时节点的再次创建
    ephemeralNodes.foreach(node => createEphemeralPath(node.path, node.data, node.createMode, isPermanent = true))
  }

  private[monad] def testCreateFailedEphemeral(path: String) {
    failedEphemeralNodes.add(new Node(path, None, CreateMode.EPHEMERAL))
  }

  sealed class EphemeralMode

  case object NormalEphemeralMode extends EphemeralMode

  case object SequenceEphemeralMode extends EphemeralMode

}
