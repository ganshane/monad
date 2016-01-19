// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai.
 * site: http://www.ganshane.com
 */
package monad.api.services


import monad.api.model.SearchResult
import monad.face.model.IdShardResult
import monad.support.services.ServiceLifecycle
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.search.highlight.Highlighter

/**
 * 搜索队列
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.3
 */
trait SearcherQueue extends ServiceLifecycle {
  /**
   * 返回总的文档数目
   * @return 文档数目
   * @since 0.1
   */
  def getDocumentNum: Long

  /**
   * 进行搜索
   * @param q 关键词
   * @param start 起始位置
   * @param offset 偏移量
   * @param sortStr 排序字符串
   * @return 搜索结果
   * @since 0.1
   * @see SearchResult
   */
  def search(q: String, start: Int, offset: Int, sortStr: String): SearchResult

  def idSearch(q: String): IdShardResult

  /**
   * 频次分析
   * @param q 查询语句
   * @param field 频次分析的字段
   * @param upper 频次的上限
   * @param lower 频次的下限
   * @return 频次分析结果
   * @since 2.1
   */
  def facetSearch(q: String, field: String, upper: Int, lower: Int): SearchResult

  /**
   * create highlighter
   * @param q query keyword
   * @return highlighter instance
   * @since 0.1
   */
  def createHighlighter(q: String): (Highlighter, Analyzer)
}
