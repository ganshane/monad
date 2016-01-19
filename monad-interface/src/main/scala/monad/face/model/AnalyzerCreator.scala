// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import monad.face.services.MonadFaceExceptionCode
import monad.support.services.MonadException
import org.apache.lucene.analysis.Analyzer

/**
 * 分词器的创建
 * @author jcai
 */
object AnalyzerCreator {
  def create(analyzerType: AnalyzerType) = {
    if (analyzerType == null) {
      throw new MonadException("analyzerType is null!",
        MonadFaceExceptionCode.ANALYZER_TYPE_IS_NULL
      )
    }

    val clazz = analyzerType.clazz
    clazz.newInstance().asInstanceOf[Analyzer]
  }
}
