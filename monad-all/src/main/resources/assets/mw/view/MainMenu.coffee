# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com

# 主界面左侧的菜单
Ext.define 'mw.view.MainMenu'
    extend: 'Ext.tree.Panel'
    alias : 'widget.MainMenu'
    rootVisible:false
    root:
        text: 'monad'
        id: 'root'
        expanded: true
    store:
        store:'tree'
        autoSync:true
        proxy:
            type:'direct'
            directFn:direct.web_Start.getMenu
