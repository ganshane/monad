// Copyright 2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

import monad.face.model.{ResourceDefinition, IndexEvent}
import org.apache.lucene.document.Document

/**
 * 文档创建
 * @author jcai
 */
trait DocumentCreator{
    def newDocument(event:IndexEvent):Document
}
trait DocumentSource{
    def newDocument(event:IndexEvent):Document
}
