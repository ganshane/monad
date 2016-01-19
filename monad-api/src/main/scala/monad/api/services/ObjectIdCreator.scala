// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.services

/**
 * object id的创建接口
 * @author jcai
 */
trait ObjectIdCreator {
  /**
   * 对象ID转换成字符串
   * @param objectId 对象ID
   * @return 字符串
   */
  def objectIdToString(objectId: Array[Byte]): String

  /**
   * 字符串转换为对象ID
   * @param str 字符串
   * @return 数据
   */
  def stringToObjectId(str: String): Array[Byte]
}
