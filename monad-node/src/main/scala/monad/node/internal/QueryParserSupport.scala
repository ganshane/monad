// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import java.util

import monad.face.MonadFaceConstants
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.model.{AnalyzerCreator, ColumnType, ResourceDefinition}
import monad.face.services.ResourceDefinitionConversions._
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.queryparser.classic.QueryParser.Operator
import org.apache.lucene.search.{NumericRangeQuery, Query}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * 查询分析器支持
 * @author jcai
 */
trait QueryParserSupport {
  private lazy val analyzerObj = AnalyzerCreator.create(rd.search.analyzer)
  private val logger = LoggerFactory getLogger getClass
  private val boostProperties = new java.util.HashMap[String, java.lang.Float]()
  private var defaultSearchFields = Array[String]()
  private var analyzer: PerFieldAnalyzerWrapper = _
  private var numericProperties = Map[String, ResourceProperty]()
  private var rd: ResourceDefinition = _

  def initQueryParser(rd: ResourceDefinition) {
    this.rd = rd
    initParserParameters()
  }

  private def initParserParameters() {
    val keyword = new KeywordAnalyzer()
    val fieldAnalyzers = new util.HashMap[String, Analyzer]()
    rd.properties.foreach(col => {
      defaultSearchFields = defaultSearchFields :+ col.name
      if (col.isKeyword) {
        //针对keyword
        fieldAnalyzers.put(col.name, keyword)
      }

      if (col.isNumeric) {
        //is numeric?
        numericProperties = numericProperties + (col.name -> col)
      }
      if (col.boost != 1.0f) {
        boostProperties.put(col.name, col.boost)
      }
    })
    analyzer = new PerFieldAnalyzerWrapper(this.analyzerObj, fieldAnalyzers)
  }

  protected def createParser(fun: Option[PartialFunction[(String, String), Query]] = None) = {
    val funIsDefine = fun.isDefined
    val parser = new MultiFieldQueryParser(this.defaultSearchFields, analyzer, boostProperties) {

      //针对数字类型的term查询，转换为数字方式查询
      override def newTermQuery(term: Term): Query = {
        val field = term.field().intern()
        val t = (field, term.text())
        if (funIsDefine && fun.get.isDefinedAt(t)) {
          return fun.get.apply(t)
        }
        val option = numericProperties.get(field)
        if (option.isDefined) {
          //数字类型的特殊处理
          option.get.columnType match {
            case ColumnType.Long =>
              try {
                val value = term.text().toLong
                return NumericRangeQuery.newLongRange(field, value, value, true, true);
              } catch {
                case e: NumberFormatException =>
                //ignore
              }
            case ColumnType.Int | ColumnType.Date =>
              try {
                val value = term.text().toInt
                return NumericRangeQuery.newIntRange(field, value, value, true, true)
                //return new TermQuery(new Term(field,NumericUtils.intToPrefixCoded(value)))
              } catch {
                case e: NumberFormatException =>
                //ignore
              }
            case other =>
              logger.error("column type[{}] not supported", other)
          }
        }
        //加入查询更新时间字段、以及ObjectId字段
        if (field == MonadFaceConstants.UPDATE_TIME_FIELD_NAME) {
          val value = term.text().toInt
          //val value = DataTypeUtils.dateToInt(term.text().toInt)
          return NumericRangeQuery.newIntRange(field, value, value, true, true)
          //return new TermQuery(new Term(field,NumericUtils.intToPrefixCoded(DataTypeUtils.dateToInt(value))))
        } else if (field == MonadFaceConstants.OBJECT_ID_FIELD_NAME) {
          val value = term.text().toInt
          return NumericRangeQuery.newIntRange(field, value, value, true, true)
          //return new TermQuery(new Term(field,NumericUtils.intToPrefixCoded(value)))
        }

        super.newTermQuery(term)
      }


      override def newRangeQuery(field: String, part1: String, part2: String, startInclusive: Boolean, endInclusive: Boolean): Query = {
        super.newRangeQuery(field, part1, part2, startInclusive, endInclusive)
        if (field == null) {
          return super.newRangeQuery(field, part1, part2, startInclusive, endInclusive)
        }

        val option = numericProperties.get(field)
        if (option.isDefined) {
          option.get.columnType match {
            case ColumnType.Long =>
              return NumericRangeQuery.newLongRange(field, part1.toLong, part2.toLong, startInclusive, endInclusive)
            case ColumnType.Int | ColumnType.Date =>
              return NumericRangeQuery.newIntRange(field, part1.toInt, part2.toInt, startInclusive, endInclusive)
            case other =>
              logger.warn("wrong query for field:{}", field)
          }
        } else if (field == MonadFaceConstants.UPDATE_TIME_FIELD_NAME) {
          val begin = part1.toInt //DataTypeUtils.convertDateAsInt(part1.toInt)
          val end = part1.toInt //DataTypeUtils.convertDateAsInt(part2.toInt)
          return NumericRangeQuery.newIntRange(field, begin, end, startInclusive, endInclusive)
        }
        super.newRangeQuery(field, part1, part2, startInclusive, endInclusive)
      }
    }
    parser.setDefaultOperator(Operator.AND)
    //		parser.setEnablePositionIncrements(true)

    parser
  }
}
