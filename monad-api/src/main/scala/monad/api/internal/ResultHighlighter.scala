// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

/**
 * 针对结果的高亮显示处理
 * @author jcai
 */
trait ResultHighlighter {
  /**
   * 高亮显示
   * @param fieldName 某一字段的名称
   * @param text 待处理的内容
   * @param maxNumFragments 最大的段
   * @return 高亮处理后的文字
   */
  def highlight(fieldName: String, text: String, maxNumFragments: Int): String
}
