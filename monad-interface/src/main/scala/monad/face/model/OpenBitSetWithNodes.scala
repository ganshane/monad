// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import org.apache.lucene.util.OpenBitSet
import org.apache.tapestry5.json.JSONArray
import com.google.gson.JsonArray

/**
  * 针对节点返回同时附带节点信息
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  */
class OpenBitSetWithNodes(val bitSet:OpenBitSet){
     var nodesAll:Int = 0
     var nodesSuccess:Int = 0
     var nodesSuccessInfo :JsonArray = _
     var nodesError:Int = 0
 }
