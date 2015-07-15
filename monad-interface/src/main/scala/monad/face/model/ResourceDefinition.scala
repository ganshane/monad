// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import java.util
import javax.xml.bind.annotation._

import monad.face.model.ResourceDefinition._


/**
 * resource definition model
 * @author jcai
 */
object ResourceDefinition {

  /**
   * JDBC连接属性
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class Jdbc {
    /**
     * 驱动类
     */
    var driver: String = _
    /**
     * 连接的URL
     */
    var url: String = _
    /**
     * 数据库用户名
     */
    var user: String = _
    /**
     * 连接密码
     */
    var password: String = _
    /**
     * 批的大小
     */
    @XmlElement(name = "batch_size")
    var batchSize: Int = 5000
    /**
     * 连接的SQL
     */
    var sql: String = _
  }

  /**
   * 索引的定义
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class Index {
    /**
     * 所采用的分词类
     */
    @XmlElement(name = "analyzer")
    var analyzer: AnalyzerType = _
  }

  /**
   * 搜索的定义
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class Search {
    /**
     * 搜索时候用的分词器
     */
    @XmlElement(name = "analyzer")
    var analyzer: AnalyzerType = _
  }

  /**
   * 同步的定义
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class Sync {
    /**
     * 同步的定时器
     */
    @XmlElement(name = "cron")
    var cron: String = "0 0 1 * * ? *"
    /**
     * 同步时候抓取间隔，单位为 分钟
     */
    @XmlElement(name = "interval")
    var interval: Float = 60

    /**
     * 同步策略
     */
    @XmlElement(name = "policy")
    var policy: SyncPolicy = SyncPolicy.Incremental
    /**
     * 数据预处理器
     */
    @XmlElement(name = "preprocessor")
    var preprocessor: String = _
    /**
     * 是否显示异常数据信息
     */
    @XmlElement(name = "show_bad_record_exception")
    var showBadRecordException: Boolean = true

    /**
     * 指定编码
     */
    @XmlElement(name = "encoding")
    var encoding: String = _

    /**
     * jdbc连接信息
     */
    @XmlElement(name = "jdbc")
    var jdbc: Jdbc = _
  }

  /**
   * 资源属性
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class ResourceProperty {
    /**
     * 属性名称
     */
    @XmlAttribute(name = "name")
    var name: String = _
    /**
     * 属性中文名称
     */
    @XmlAttribute(name = "cn_name")
    var cnName: String = _
    /**
     * 属性在列表展示的名称
     */
    @XmlAttribute(name = "list_name")
    var listName: String = _
    /**
     * 关联的规范词名
     */
    @XmlAttribute(name = "dic")
    var dic: String = _
    /**
     * 索引的类型,可以选择Text Keyword
     */
    @XmlAttribute(name = "index_type")
    var indexType: IndexType = IndexType.Text
    /** 是否为主键 **/
    @XmlAttribute(name = "primary_key")
    var primaryKey: Boolean = _
    /** 是否为增量列 **/
    @XmlAttribute(name = "modify_key")
    var modifyKey: Boolean = _
    /** 是否为默认查询字段 **/
    @XmlAttribute(name = "default_query")
    var defaultQuery: Boolean = _
    /** 查询类型 **/
    @XmlAttribute(name = "query_type")
    var queryType: QueryType = QueryType.String
    /** 列的属性，可选 Int Long String Date **/
    @XmlAttribute(name = "column_type")
    var columnType: ColumnType = ColumnType.String
    /** 格式，通常用作，字符型的日期类型,此值是将数据库中格式转换 **/
    @XmlAttribute(name = "db_format")
    var dbFormat: String = _
    /** 格式，通常用作，字符型的日期类型,此值将影响API的输出格式 **/
    @XmlAttribute(name = "api_format")
    var apiFormat: String = _
    /**
    该属性的一些特性定义,按照从右向左二进制的位数来定义
          第1位 是否为主键字段,兼容 3.2 以下版本的定义,将在4.0版本删除 primary_key 属性
          第2位 是否为增量字段 在4.0版本将删除 modify_key 属性
          第3位 是否为默认查询，用于精确查询的页面 在4.0版本将删除 default_query 属性
          第4位 是否为身份证号码,since 3.2.1

          譬如：
            mark=5,那么二进制是 101 那么代表是主键字段，也是默认查询
            mark=12,那么二进制是 1100 那么代表是默认查询字段，也是身份证号码字段
      */
    @deprecated(since = "5.1",message = "使用object_category进行代替")
    @XmlAttribute(name = "mark")
    var mark: Int = 0
    @XmlAttribute(name = "boost")
    var boost: Float = 1.0f
    @XmlAttribute(name = "object_category")
    var objectCategory: ObjectCategory = _



    //仅仅方便在数据同步时候得到设置的编码
    var resourceDefinition: ResourceDefinition = _
  }

  /**
   * 特征资源属性
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class ResourceDynamicType {
    /**
     * 本资源的属性名称
     */
    @XmlAttribute(name = "desc_format")
    var descFormat: String = _

    @XmlElement(name = "property")
    var properties = new util.ArrayList[ResourceTraitProperty]()
  }

  /**
   * 特征资源属性
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class ResourceTraitProperty {
    /**
     * 本资源的属性名称
     */
    @XmlAttribute(name = "name")
    var name: String = _
    /**
     * 对应的特征属性
     * @see ResourceRelation
     */
    @XmlAttribute(name = "trait")
    var traitProperty: String = _
  }

  /**
   * 关联的特征
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  class Rel {
    /**
     * 特征类型
     */
    @XmlAttribute(name = "name")
    var name: String = _
    /**
     * 特征列集合
     */
    @XmlElement(name = "property")
    var properties = new util.ArrayList[ResourceTraitProperty]()
  }

}

/**
 * 资源定义
 */
@XmlRootElement(name = "resource")
@XmlAccessorType(XmlAccessType.FIELD)
class ResourceDefinition {
  /**
   * 资源名称
   */
  @XmlAttribute(name = "name")
  var name: String = _
  /**
   * 定义资源的类型
   */
  @XmlAttribute(name = "type")
  var resourceType: ResourceType = ResourceType.Real
  /**
   * 目标资源，用来把本资源数据提供给其他资源的配置
   */
  @XmlAttribute(name = "target")
  var targetResource: String = _
  /**
   * 资源中文名称
   */
  @XmlAttribute(name = "cn_name")
  var cnName: String = _
  /**
   * 是否为动态资源
   */
  @XmlAttribute(name = "dynamic")
  var dynamic: Boolean = _
  /**
   * 是否为共享资源
   */
  @XmlAttribute(name = "share")
  var share: Boolean = _
  /**
   * 是否要保存本资源数据
   */
  @XmlAttribute(name = "save")
  var save: Boolean = true

  /**
   * 索引定义
   */
  @XmlElement(name = "index")
  var index: Index = _
  /**
   * 搜索配置
   */
  @XmlElement(name = "search")
  var search: Search = _
  /**
   * 同步属性配置
   */
  @XmlElement(name = "sync")
  var sync: Sync = _
  /**
   * 资源属性集合
   */
  @XmlElementWrapper(name = "properties")
  @XmlElement(name = "property")
  var properties = new util.ArrayList[ResourceProperty]()
  /**
   * 资源动态特征属性映射
   */
  @XmlElement(name = "dynamic")
  var dynamicType: ResourceDynamicType = _
  /**
   * 支持的关系查询集合
   */
  @XmlElementWrapper(name = "relations")
  @XmlElement(name = "rel")
  var relations = new util.ArrayList[Rel]()

}