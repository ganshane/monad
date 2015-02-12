// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/**
 * 启动程序
 * @param app_path 程序的路径
 * @param version 程序的版本
 */
function startApp(app_path,version){
    Ext.Loader.setPath('mw',app_path);
    Ext.require('mw.Bootstrap',function(){
        mw.Bootstrap.start(app_path,version)
    });
}
