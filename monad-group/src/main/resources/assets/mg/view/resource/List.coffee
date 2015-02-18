###
 Copyright 2012 The EGF IT Software Department.
 site: http://www.ganshane.com
###

Ext.define 'mg.view.resource.List' ,
    extend: 'Ext.grid.Panel'
    alias : 'widget.resourcelist'
    store: 'Resources'
    #列定义
    columns: [
            {header: '名称',  dataIndex: 'name',  flex: 1},
            {header: '中文名称', dataIndex: 'cnName', flex: 1},
            {header: 'ID序列', dataIndex: 'quantity', flex: 1},
            {header: '最大值', dataIndex: 'maxValue', flex: 1}
        ]

    initComponent: ->
      Ext.apply this,
        # 按钮
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            items: [{
                id:'addResourceBtn'
                icon: ICON_DIR+'/add.png'
                iconCls: 'x-btn-text-icon'
                text: '增加资源'
            },
            {
                id:'deleteResourceBtn'
                icon: ICON_DIR+'/delete.png'
                iconCls: 'x-btn-text-icon'
                text: '删除资源'
            },
            {
                id:'resyncResourceBtn'
                icon: ICON_DIR+'/application_lightning.png'
                iconCls: 'x-btn-text-icon'
                text: '重新抽取'
            },
            {
                id:'refreshResourceBtn'
                icon: ICON_DIR+'/arrow_refresh.png'
                iconCls: 'x-btn-text-icon'
                text: '刷新'
            }]
        }]
      this.callParent arguments
    listeners:
        viewready:(view,obj) ->
###
            task =
                run: ->
                    view.getStore().each (rec)->
                        direct.group_ResourceAction.findExtensionValue rec.data.name,(provider,response) ->
                                if response.type is 'rpc'
                                    rec.set('quantity',response.result.quantity)
                                    rec.set('maxValue',response.result.maxValue)
                interval: 30000
            Ext.TaskManager.start(task);
###
