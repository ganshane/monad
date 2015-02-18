###
 Copyright 2012 The EGF IT Software Department.
 site: http://www.ganshane.com
###

Ext.define 'mg.view.resource.Add',
    extend: 'Ext.window.Window',
    alias : 'widget.resourceadd',
    title : '增加资源',
    xml:'',
    modal: true,
    autoShow: true,
    bodyStyle:'padding:5px',
    width: 750,
    height: 500,
    minWidth: 300,
    minHeight: 200,
    layout: 'fit',
    collapsible: true,
    animCollapse: true,
    maximizable: true,

    items:
       xtype:'form',
       plain: true,
       border: 0,
       bodyPadding: 5,

       fieldDefaults:
           labelWidth: 55,
           anchor: '100%'

       layout:
           type: 'vbox',
           align: 'stretch'  #Child items are stretched to full width
       items:
           xtype: 'textarea',
           fieldLabel: 'XML 内容',
           hideLabel: true,
           name: 'xml',
           id:'xmlField',
           #style: 'margin:0', # Remove default margin
           flex: 1  # Take up all *remaining* vertical space
    setValue: (xml) ->
        Ext.getCmp("xmlField").setValue(xml);
    initComponent: ->
        this.buttons = [{
                text: '保存',
                action: 'save',
                handler: this.save,
                scope: this
            },
            {
                text: '取消',
                scope: this,
                handler: this.close
            }];
        this.callParent(arguments);
    save: ->
        myMask = new Ext.LoadMask(this,{msg:"Please wait..."});
        myMask.show();
        value = Ext.getCmp("xmlField").getValue();
        me = this;
        direct.group_ResourceAction.create value,(provider, response)->
            if response.type is 'exception'
                Ext.Msg.show
                     title:'保存失败',
                     msg: response.message,
                     buttons: Ext.Msg.OK,
                     icon: Ext.Msg.ERROR
            else
                #更新成功，重新加载资源
                me.close();
                Ext.getCmp("resourcelist").getStore().load();

            myMask.hide();
    close:->
        this.callParent arguments
