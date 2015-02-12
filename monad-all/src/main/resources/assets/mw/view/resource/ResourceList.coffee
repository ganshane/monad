# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com


# 资源列表
Ext.define 'mw.view.resource.ResourceList'
    extend: 'Ext.tree.Panel'
    alias : 'widget.ResourceList'
    rootVisible:false
    defaults:
        autoScroll: true
    fields:[{name:'text',persist:false}]
    constructor: (config)->
        this.store =
            store:'tree'
            root:
               expanded: true
               children: config.resources
        this.callParent(arguments)


