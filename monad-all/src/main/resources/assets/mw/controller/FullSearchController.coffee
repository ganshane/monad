Ext.define 'mw.controller.FullSearchController'
    extend: 'Ext.app.Controller'
    views: [
        'full.FullSearchLayout',
        'full.FullSearchField',
        'resource.ResourceList',
    ]
    init: ->
        this.control
            '#searchfield':
                search:this.fullSearchCount
            '#resourceListMenu':
                itemclick:this.selectResource
            '#SearchResultGridPanel':
                itemdblclick:this.selectRecord

    #
    #  执行全文搜索
    #  @param rd 资源定义类
    #  @param query 查询的语句
    #
    fullSearch: (node,query) ->
        cmp = Ext.getCmp("searchResult")
        rd = node.data
        group = node.group
        cmp.removeAll()
        #创建结果的Panel
        panel = Ext.create("mw.view.resource.SearchResultGridPanel",rd.name,rd.properties);
        panel.id="SearchResultGridPanel"
        cmp.add(panel);
        store = panel.getStore();
        store.proxy.url=group.apiUrl+'/search';
        Ext.apply(store.proxy.extraParams, {i:rd.name,q:query});
        store.load();
    #选择了某一资源
    selectResource:(view,record,item,index,e) ->
        value = Ext.getCmp("searchfield").getValue();
        if(value && value != '')
            this.fullSearch(record.raw,value);
        else
            Ext.Msg.alert("Monad","请输入搜索内容");
    #选中结果中的记录
    selectRecord:(dv, record, item, index, e) ->
        Ext.getBody().mask();
        win= Ext.create("mw.view.resource.ShowRecord",dv.panel.properties);
        win.down('form').loadRecord(record)

    #全文检索资源的记录数
    fullSearchCount: (view,node,store) ->
      original = store.tree.getNodeById(node.id)
      parentNode = original.parentNode;
      original.set('loading', true);
      if (parentNode)
         Ext.data.JsonP.request
             url:node.group.apiUrl+'/count',
             params:
                i:node.id.substring(2)
                q:view.getValue()
             success:(response) ->
                if(response && response.status == 0)
                    original.set("text",node.text+"("+response.total+")")
                original.set('loading',false)

