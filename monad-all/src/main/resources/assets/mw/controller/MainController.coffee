# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com



# 整个应用程序的主要控制器
Ext.define 'mw.controller.MainController',
    extend: 'Ext.app.Controller'
    views: ['Main','MainMenu']

    #
    #  创建全文检索的布局
    #  @param city 城市的标示
    #
    createFullSearchLayout:(groupConfig)->
        #删除主界面内容
        cmp = Ext.getCmp("mainContent")
        cmp.removeAll();
        #通过direct得到所有资源列表
        direct.web_Start.getResources groupConfig.id,(provider,response) ->
            if response.type is 'rpc'
                cmp.add Ext.create 'mw.view.full.FullSearchLayout',
                    resources:
                        Ext.Array.map response.result,(r) ->
                            id:'r_'+r.name
                            text:r.cnName
                            leaf:true
                            group:groupConfig
                            data:r
    #选择菜单
    selectMenu:(view,record,item,index,e) ->
        if /^g_/.test(record.get("id")) #选择某一城市
            this.createFullSearchLayout(record.raw.config)
    #初始化
    init: ->
        this.control
            '#MainMenu':
                itemclick: this.selectMenu
