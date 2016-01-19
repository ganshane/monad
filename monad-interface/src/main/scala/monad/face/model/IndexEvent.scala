// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import com.google.gson.JsonObject

/**
 * 索引的事件
 * @author jcai
 */
class IndexEvent {
    //资源名称
    var resource: ResourceDefinition = null
    //行ID
    var id: Int = -1
    //分析使用的对象ID
    var objectId:Option[Int] = None
    //数据
    var row: JsonObject = null
    var command:Int = _
    //提交标记
    var commitFlag:Boolean =false
    var commitSeq:Long = -1
    var version:Int = -1
    def reset(){
        resource =null
        row = null
        id= -1
        commitFlag=false
        commitSeq = -1
        version = -1
        command =  -1
        objectId = None
    }
}
