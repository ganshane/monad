// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.face.model

import monad.face.MonadFaceConstants
import monad.face.services.MonadFaceExceptionCode
import monad.support.services.MonadException
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.util.Version

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
    (analyzerType.constructorLen match {
      case 0 =>
        clazz.newInstance()
      case 1 =>
        clazz.
          getConstructor(classOf[Version]).
          newInstance(MonadFaceConstants.LUCENE_VERSION)
    }).asInstanceOf[Analyzer]
  }
}
