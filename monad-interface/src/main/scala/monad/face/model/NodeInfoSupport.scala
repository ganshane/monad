// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model

import com.google.gson.JsonArray

/**
 * 节点信息返回的支持类
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
trait NodeInfoSupport extends java.io.Serializable{
    /**
     * 设置总节点数目
     * @param count 总节点数目
     */
    def setNodeCount(count:Int)

    /**
     * 设置成功的节点数目
     * @param count 成功的节点数
     */
    def setNodeSuccess(count:Int)

    def setNodeSuccess(servers:JsonArray)

    /**
     * 设置发生错误的节点数目
     * @param count 发生错误的节点数目
     */
    def setNodeError(count:Int)
}
