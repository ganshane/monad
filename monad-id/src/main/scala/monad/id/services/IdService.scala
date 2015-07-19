package monad.id.services

import monad.protocol.internal.InternalIdProto.IdCategory

/**
 * id service
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
trait IdService {
  /**
   * 得到一个对象的序列值
   * @param category 类别
   * @param idLabel 对象字符串
   * @return 对象的序列
   */
  def getOrAddId(category: IdCategory,idLabel:String):Option[Int]
  def get(category: IdCategory,idLabel:String):Option[Int]

  /**
   * 通过给定的序列得到对应的对象字符串
   * @param idOrd 对象序列
   * @return 对象字符串
   */
  def getIdLabel(category:IdCategory,idOrd:Int):Option[String]
}
