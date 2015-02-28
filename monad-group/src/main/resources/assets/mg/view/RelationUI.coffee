
Ext.define 'mg.view.RelationUI',
    extend: 'Ext.panel.Panel'
    layout:'fit'
    listeners:
        afterRender:(view,obj) ->
            unless view.loadMask
                view.loadMask = new Ext.LoadMask(view,{msg:"Please wait..."})
            view.loadMask.show();
            me = view
            direct.group_RelationAction.getRelation (provider,response) ->
                if response.type is 'rpc'
                    el = Ext.getCmp('relation_value')
                    el.setValue(response.result)
                me.loadMask.hide();
    items:
        xtype:'textarea'
        id:'relation_value'
    dockedItems:
            xtype: 'toolbar',
            dock: 'top',
            items:
                xtype:'button'
                id:'btn_save_relation'
                icon:ICON_DIR+"/page_save.png"
                iconCls: 'x-btn-text-icon'
                text:'保存'
