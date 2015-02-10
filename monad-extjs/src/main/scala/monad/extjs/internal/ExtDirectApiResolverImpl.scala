// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal

import org.apache.tapestry5.services.ComponentClassResolver
import collection.JavaConversions._
import monad.extjs.annotations.ExtDirectMethod
import com.google.gson.{JsonObject, JsonArray}
import monad.extjs.services.ExtDirectApiResolver
import org.apache.tapestry5.services.assets.AssetPathConstructor

/**
 * 得到所有的Extjs的API
 * @author jcai
 */
class ExtDirectApiResolverImpl(componentClassResolver:ComponentClassResolver,
                               assetPathConstructor:AssetPathConstructor)
    extends ExtDirectApiResolver{
    val pageNames = componentClassResolver.getPageNames

    val actions = pageNames.foldLeft(new JsonObject)((json,page)=>{
        val clazz=Thread.currentThread().
          getContextClassLoader.loadClass(componentClassResolver.resolvePageNameToClassName(page))
        val methodsJson = clazz.getMethods.
          filter(_.getAnnotation(classOf[ExtDirectMethod]) != null).
          foldLeft(new JsonArray)((arr,method)=>{
            val methodJson = new JsonObject
            methodJson.addProperty("name",method.getName)
            methodJson.addProperty("len",method.getParameterTypes.size)
            arr.add(methodJson)
            arr
        })
        if (methodsJson.size() > 0 ){
            json.add(page.replace('/','_'),methodsJson)
        }

        json
    })

    val appPath=assetPathConstructor.constructAssetPath("extjs_static","icons")
    val uxPath=assetPathConstructor.constructAssetPath("extjs_static","examples/ux")
    val sDotGifPath = assetPathConstructor.constructAssetPath("extjs_static","resources/themes/images/default/tree/s.gif")
    //Ext.onReady(function(){
    var jsContent:String =
        """var ICON_DIR='"""+appPath+"""';
Ext.ns("Ext.app");
Ext.app.REMOTING_API="""
    val json = new JsonObject
    json.addProperty("url","/api/router")
    json.addProperty("type","remoting")
    json.addProperty("namespace","direct")
    json.addProperty("enableBuffer",false)
    json.add("actions",actions)

    jsContent += json.toString
    jsContent += """;Ext.require(['Ext.direct.Manager']);
Ext.Loader.setConfig({enabled: true,disableCaching:false});
Ext.Ajax.defaultHeaders = {"X-Extjs-Request":"1.3"};
if(Ext.isIE6 || Ext.isIE7) Ext.BLANK_IMAGE_URL='%s';
var uxPath='%s';
Ext.Loader.setPath('Ext.ux',uxPath);
Ext.direct.Manager.addProvider(Ext.app.REMOTING_API);
                 """.format(sDotGifPath,uxPath)

    def getActions:JsonObject=actions
    def toJs:String=jsContent
}
