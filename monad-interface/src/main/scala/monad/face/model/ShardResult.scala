// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import com.google.gson.JsonArray
import monad.face.internal.MonadSparseFixedBitSet

/**
 * shard result
 * @author jcai
 * @version 0.1
 */
class ShardResult extends Serializable{
    var totalRecord:Int = 0
    var results:Array[(Int,Float)] = _
    var facetArr:Array[String] = _
    var maxScore:Float = _
    var serverHash:Short = 0
    var maxDoc:Int= 0
}
class ShardResultCollect extends ShardResult{
    var shardResults:Array[ShardResult] = null
    var nodesAll:Int = 0
    var nodesSuccess:Int = 0
    var nodesSuccessInfo:JsonArray = _
    var nodesError:Int = 0
}
class IdShardResult extends Serializable{
    var data: MonadSparseFixedBitSet = _
    var region:Short = 0
}
class IdShardResultCollect extends IdShardResult{
    var results:Array[IdShardResult] = _
    var nodesAll:Int = 0
    var nodesSuccess:Int = 0
    var nodesSuccessInfo:JsonArray = _
    var nodesError:Int = 0
}
class IdSeqShardResult(val seq:Int,val region:Short) extends Serializable{
}
