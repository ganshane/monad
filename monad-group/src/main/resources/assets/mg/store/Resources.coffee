###
 Copyright 2012 The EGF IT Software Department.
 site: http://www.ganshane.com
###

Ext.define 'mg.store.Resources',
    extend: 'Ext.data.Store'
    model: 'mg.model.Resource'
    autoLoad: false,
    proxy:
        type:'direct',
        api:
            read: direct.group_ResourceAction.findAll
            destroy: direct.group_ResourceAction.destroyRecord
        directFn:direct.group_ResourceAction.findAll
        listeners:
            exception: (proxy, response, operation) ->
                Ext.log(response.where)
                Ext.MessageBox.show
                    title: '请求失败',
                    msg: operation.getError(),
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
