// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import scala.collection.JavaConversions._

/**
 * API执行结果
 * @author jcai
 */
class JsonApiResponse(result: Any) {
  var success: Boolean = true
  var status: Int = 0
  var msg: String = _
  private var data: Any = _

  data = result match {
    case theMap: Map[_, _] =>
      mapAsJavaMap(theMap)
    case it: Iterable[_] =>
      asJavaIterable(it)
    case it: Iterator[_] =>
      asJavaIterable(it.toIterable)
    case other =>
      other
  }

  def getData = data
}
