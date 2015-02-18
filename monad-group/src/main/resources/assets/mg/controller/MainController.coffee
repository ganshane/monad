Ext.define 'mg.controller.MainController',
    extend: 'Ext.app.Controller'
    uses:["mg.view.resource.List"]
    clickMenu:(view,rec,item,index,eventObj)->
        switch rec.get("id")
            when "m_resource" then this.showResourceUI()
            when "m_relation" then this.showRelationUI()
            when "m_dynamic" then this.showDynamicUI()
    # 显示动态信息UI
    showDynamicUI: ->
        wrc=Ext.getCmp("main");
        wrc.removeAll();
        wrc.setTitle("关系列表");
        panel = Ext.create 'mg.view.DynamicUI',id:'DynamicUI'
        wrc.add panel
    # 显示关系的UI
    showRelationUI: ->
        wrc=Ext.getCmp("main");
        wrc.removeAll();
        wrc.setTitle("关系列表");
        panel = Ext.create 'mg.view.RelationUI',id:'RelationUI'
        wrc.add panel

    #显示资源管理的主界面
    showResourceUI: ->
        #主要程序界面
        wrc=Ext.getCmp("main");
        wrc.removeAll();
        wrc.setTitle("资源列表");
        panel = Ext.create "mg.view.resource.List",id:'resourcelist'
        wrc.add(panel);
        panel.getStore().load();
    #保存关系定义
    saveRelation: ->
        el = Ext.getCmp('relation_value')

    init: ->
        this.control
            '#groupmenu':
                itemclick: this.clickMenu
            '#btn_save_relation':
                click: this.saveRelation
