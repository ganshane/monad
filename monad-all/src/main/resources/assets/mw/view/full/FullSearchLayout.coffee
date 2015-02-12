# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com

# 搜索结果的布局
Ext.define 'mw.view.full.FullSearchLayout'
    extend: 'Ext.panel.Panel'
    layout: 'border'
    bodyBorder: false
    config:
        resources:[]
    northItem:
        region:'north'
        height: 50
    westItem:
        id: 'resourceListMenu'
        region:'west'
        collapsible: true
        split: true
        title: '资源列表'
        xtype:"ResourceList"
        resource:[]
        width:200
    centerItem:
        id: 'searchResult'
        region:'center'
        xtype: 'panel'
        layout:'fit'
        dockedItems:
            dock: 'top'
            xtype: 'toolbar'
            items:
                id:'searchfield'
                width: 400
                fieldLabel: '搜索'
                labelWidth: 50,
                xtype:'FullSearchField'
    initComponent: ->
        this.westItem.resources = this.resources
        this.items = [this.westItem,this.centerItem]
        this.callParent arguments
