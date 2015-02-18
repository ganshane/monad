
# 关系设置的控制器
Ext.define 'mg.controller.RelationController',
    extend: 'Ext.app.Controller'
    views: ['RelationUI']
    init: ->
        this.control
            '#btn_save_relation':
                click: this.saveRelation
    saveRelation: ->
        ui = Ext.getCmp('RelationUI')
        ui.loadMask.show()
        el = Ext.getCmp('relation_value')
        direct.group_RelationAction.create el.getValue(),(provider, response)->
            if response.type is 'exception'
                Ext.Msg.show
                     title:'保存失败',
                     msg: response.message,
                     buttons: Ext.Msg.OK,
                     icon: Ext.Msg.ERROR
            ui.loadMask.hide()

