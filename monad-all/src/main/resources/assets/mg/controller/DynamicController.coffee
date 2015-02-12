
# 动态资源管理的控制器
Ext.define 'mg.controller.DynamicController',
    extend: 'Ext.app.Controller'
    views: ['DynamicUI']
    init: ->
        this.control
            '#btn_save_dynamic':
                click: this.saveDynamic
    saveDynamic: ->
        ui = Ext.getCmp('DynamicUI')
        ui.loadMask.show()
        el = Ext.getCmp('dynamic_value')
        direct.group_DynamicAction.create el.getValue(),(provider, response)->
            if response.type is 'exception'
                Ext.Msg.show
                     title:'保存失败',
                     msg: response.message,
                     buttons: Ext.Msg.OK,
                     icon: Ext.Msg.ERROR
            ui.loadMask.hide()

