// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal.support

/**
 * id buffer
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
trait IdBuffer {
  /**
   * 设置数据
   * @param bytes
   */
  def put(bytes: Array[Byte])

  /**
   * 读取某一文档对应的NoSQL的ID
   * @param docId 文档ID
   * @return NoSQL的ID值
   */
  def apply(docId: Int): Array[Byte]

  /**
   * 得到分析对象的文档ID
   * @param docId 文档ID
   * @return 分析对象的值
   */
  def getAnalyticObjectId(docId: Int): Int

  /**
   * 关闭这个buffer
   */
  def close()
}
