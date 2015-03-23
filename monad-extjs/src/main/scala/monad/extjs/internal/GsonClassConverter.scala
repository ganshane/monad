// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import java.lang.reflect.Type

import com.google.gson._

import scala.collection.JavaConversions._

/**
 * gson 和 对象之间的转换
 * @author jcai
 */
object GsonClassConverter {
    //单态线程安全的类
    //增加对JsonElement本身的序列化
    private val gson = new GsonBuilder().registerTypeHierarchyAdapter(classOf[JsonElement],new JsonSerializer[JsonElement] {
        def serialize(src: JsonElement, typeOfSrc: Type, context: JsonSerializationContext) = {
            src
        }
    }).create()
    /**
     * 转换为JSON对象
     */
    def toJSON(obj: AnyRef): JsonElement = {
        val objConverted = obj match {
            case theMap: Map[_, _] =>
                mapAsJavaMap(theMap)
            case it: Iterable[_] =>
                asJavaIterable(it)
            case it: Iterator[_] =>
                asJavaIterable(it.toIterable)
            case other =>
                other
        }
        gson.toJsonTree(objConverted)
    }

    /**
     * 从JSON中获取对象
     */
    def fromJSON[T](json: String)(implicit m: Manifest[T]): T = {
        //obtain type parameter
        val clazz = m.runtimeClass.asInstanceOf[Class[T]]
        gson.fromJson(json, clazz)
    }
}
