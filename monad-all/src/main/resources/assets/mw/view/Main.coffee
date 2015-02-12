# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com


# 应用程序的主界面
Ext.define 'mw.view.Main'
    extend: 'Ext.container.Viewport'
    alias : 'widget.main'
    layout: 'border'
    # 上部的banner
    northItem:
        region:'north'
        xtype: 'panel'
        height: 50
        html : "<span class='banner'>Monad一键搜索</span>"
    # 左边的部分
    westItem:
        id: 'MainMenu'
        xtype:'MainMenu'
        region:'west'
        collapsible: true
        split: true
        title: '菜单'
        width:200
    # 中间主要内容部分
    centerItem:
        id: 'mainContent'
        region:'center'
        layout:'card'
        border: false
    # 构造主界面
    constuctor:(config) ->
        this.monadVersion = config.monadVersion;
        this.callParent(arguments)
    initComponent: ->
        this.items=[
                     this.northItem,this.westItem,this.centerItem,
                     region:'south'
                     xtype: 'toolbar'
                     layout:'fit'
                     border: true
                     style:"text-align:center"
                     html:"<div>Powered By Monad (v"+this.monadVersion+")</div>"
                    ]
        this.callParent arguments

