// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
function startMonadGroupApplication(app_path){
    Ext.Loader.setPath('mg',app_path);
    Ext.require("mg.Bootstrap",function(){
        mg.Bootstrap.initApplication(app_path)
    });
}
