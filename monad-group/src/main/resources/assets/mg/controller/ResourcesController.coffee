###
 Copyright 2012 The EGF IT Software Department.
 site: http://www.ganshane.com
###

Ext.define 'mg.controller.ResourcesController',
    extend: 'Ext.app.Controller'
    views: ['resource.List','resource.Add']
    stores:[ 'Resources']
    models: ['Resource']
    init: ->
        this.control
            '#addResourceBtn':
                click: this.addResource
                scope:this
            '#deleteResourceBtn':
                click: this.deleteResource
                scope:this
            '#resyncResourceBtn':
                click: this.resyncResource
                scope:this
            'resourcelist':
                itemdblclick: this.editResource
                scope:this
            '#refreshResourceBtn':
                click: this.refreshList
                scope:this
    # 删除资源
    deleteResource: ->
        Ext.Msg.show
             title:'要删除吗?'
             msg: '你正在删除一个资源，你真的要删除吗？'
             buttons: Ext.Msg.YESNO
             icon: Ext.Msg.QUESTION
             fn: (btn) ->
                if(btn == 'yes')
                    grid = Ext.getCmp("resourcelist")
                    sm = grid.getSelectionModel()
                    grid.store.remove(sm.getSelection())
                    grid.store.sync()
    resyncResource: ->
        grid = Ext.getCmp("resourcelist")
        sm = grid.getSelectionModel()
        selection = sm.getSelection();
        if(selection.length == 0)
            Ext.Msg.show
                title:'错误'
                msg: '请选择需要重新抽取的资源'
                buttons: Ext.Msg.OK
                icon: Ext.Msg.ERROR
        else
            Ext.Msg.show
                 title:'要重新抽取吗?'
                 msg: '重新抽取意味要删除之前的索引，并且重新导入数据和索引？'
                 buttons: Ext.Msg.YESNO
                 icon: Ext.Msg.QUESTION
                 fn: (btn) ->
                    if(btn == 'yes')
                        resources_selected = ""
                        Ext.Array.forEach selection,(item,index)->
                            if(index > 0)
                                resources_selected += ","
                            resources_selected += item.get("name")
                        myMask = new Ext.LoadMask(grid,{msg:"请稍候，正在重新加载"+resources_selected+"..."});
                        myMask.show();
                        direct.group_ResourceAction.resync resources_selected,(provider, response)->
                            if response.type is 'exception'
                                Ext.Msg.show
                                     title:'重新抽取失败',
                                     msg: response.message,
                                     buttons: Ext.Msg.OK,
                                     icon: Ext.Msg.ERROR

                            myMask.hide();
                            myMask.destroy();

    # 编辑某一资源
    editResource: (grid, record)->
        direct.group_ResourceAction.getResourceXml record.data.name,(provider, response)->
            view = Ext.widget('resourceadd');
            view.setValue(response.result)
            view.down('form');
    # 刷新
    refreshList: ->
        Ext.getCmp("resourcelist").getStore().load();
    #增加资源
    addResource: (grid, record) ->
        Ext.widget('resourceadd').down('form')
