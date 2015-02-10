// Copyright 2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.services

/**
 * RPC本地调用
 * @author jcai
 */
trait RpcLocalExecutor{
    def execute(methodId:Int,arg:Array[Any]):Object
}
