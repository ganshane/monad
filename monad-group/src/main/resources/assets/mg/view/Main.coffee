# 应用程序的主界面
Ext.define 'mg.view.Main',
    extend: 'Ext.container.Viewport'
    alias : 'widget.main'
    uses:['mg.view.GroupMenu']
    layout: 'border'
    items: [{
            region:'north'
            xtype: 'panel'
            height: 50
            html : "<span class='banner'>Monad资源管理控制台</span>"
        },
        {
            region:'west',
            split: true,
            xtype: 'groupmenu',
            title: '菜单',
            id:'groupmenu',
            width:200,
        },
        {
            region:'center',
            id: 'main',
            xtype: 'panel',
            layout: 'fit'
        }
    ]
