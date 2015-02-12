# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com

#  显示某一条信息
Ext.define "mw.view.resource.ShowRecord"
    extend: 'Ext.window.Window'
    alias : 'widget.showrecord'
    title : '显示记录'
    layout: 'fit'
    width:600
    autoScroll:true

    defaults:
        anchor: '100%'
    autoShow: true
    listeners:
        close:->
           Ext.getBody().unmask()
    constructor:(properties) ->
        #采用Column的布局方式，分两列展示
        this.subItems = Ext.Array.map properties,(pro) ->
            { xtype: 'displayfield'
            name : pro.name
            columnWidth: .5
            fieldLabel: pro.cnName}
        this.callParent(arguments);
        this
    initComponent: ->
        this.items =
              xtype: 'form'
              layout:'column'
              fieldDefaults:
                  labelAlign: 'right'
                  labelWidth: 100
                  labelStyle: 'font-weight:bold'
              items: this.subItems
        this.callParent(arguments);
