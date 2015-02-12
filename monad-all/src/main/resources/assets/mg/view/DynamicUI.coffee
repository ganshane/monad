
Ext.define 'mg.view.DynamicUI'
    extend: 'Ext.panel.Panel'
    layout:'fit'
    listeners:
        afterRender:(view,obj) ->
            unless view.loadMask
                view.loadMask = new Ext.LoadMask(view,{msg:"Please wait..."})
            view.loadMask.show();
            me = view
            direct.group_DynamicAction.getDynamic (provider,response) ->
                if response.type is 'rpc'
                    el = Ext.getCmp('dynamic_value')
                    el.setValue(response.result)
                me.loadMask.hide();
    items:
        xtype:'textarea'
        id:'dynamic_value'
    dockedItems:
            xtype: 'toolbar',
            dock: 'top',
            items:
                xtype:'button'
                id:'btn_save_dynamic'
                icon:ICON_DIR+"/page_save.png"
                iconCls: 'x-btn-text-icon'
                text:'保存'
