// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.config

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlType}

/**
 * 文字索引配置
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TextIndexConfig")
class TextIndexConfig {
  /** 索引所在目录 **/
  @XmlElement(name = "path")
  var path: String = "target/index-tmp"
  /** 进行索引写操作时候的内存缓存,此数据单位为 MB **/
  @XmlElement(name = "writer_buffer_mb")
  var writerBuffer: Int = 32
  /** 需要进行提交时候的数量 **/
  @XmlElement(name = "need_commit")
  var needCommit: Int = 10000
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TextIndexConfigSupport")
trait TextIndexConfigSupport {
  @XmlElement(name = "text")
  var textIndexConfig: TextIndexConfig = new TextIndexConfig
}
