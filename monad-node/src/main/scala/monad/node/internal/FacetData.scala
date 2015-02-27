// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import com.carrotsearch.hppc._


/**
 * Facet data
 * @author jcai
 */
object FacetData {

  class SfzhStat(var termIndex: Int, var count: Int) {
    var sfzh: String = _
    var docId: Int = 0
    var objectId: Array[Byte] = _
    var serverId: Short = _

    override def toString = "docId:" + docId + " sfzh:" + sfzh + " count:" + count + " server:" + serverId

    def increaseCount(num: Int) {
      count += num
    }
  }

}

class FacetData(maxDoc: Int) {
  /**
   * 记录了term顺序记录
   * key 为 docId,value 为 term所在的顺序位置
   */
  val termOrder = new IntIntOpenHashMap(maxDoc)
  val minDocs = new IntArrayList
  /**
   * 记录了数据为Long的数据
   */
  val terms = new ObjectArrayList[String]

  //StringArrayList
  def release {
    termOrder.clear()
    terms.clear()
  }
}
