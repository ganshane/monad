# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com

# 搜索结果的列表页展示
Ext.define 'mw.view.resource.SearchResultGridPanel'
    extend: 'Ext.grid.Panel'
    alias : 'widget.SearchResultGridPanel'
    constructor:(name,properties) ->
        this.properties = properties
        this.columns=Ext.Array.map this.properties,(pro) ->
            _hidden = (! pro.listName)
            {header:pro.cnName,dataIndex:pro.name,flex:1,hidden:_hidden}
        _fields = Ext.Array.map this.properties,(pro) ->
            {name:pro.name}
        Ext.define name+'Model'
            extend: 'Ext.data.Model'
            idProperty:'_id'
            fields: _fields
        this.store = Ext.create "Ext.data.Store"
            model:name+'Model'
            pageSize:30
            proxy:
                type:'jsonp',
                url: 'http://localhost:9080/dev/api/search'
                reader:
                    type: 'json'
                    root: 'data'
        this.bbar =
              xtype : 'pagingtoolbar'
              store : this.store
              dock : 'bottom'
              displayInfo : true
        this.callParent(arguments)
