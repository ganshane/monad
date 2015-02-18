# Group Menu Component
Ext.define "mg.view.GroupMenu",
    extend: 'Ext.tree.Panel',
    alias : 'widget.groupmenu',
    rootVisible:false,
    root:
        text:'root',
        expanded:true,
        children:[{
                id:"m_resource",
                text:"资源管理",
                leaf:true
            }
            {
                id:"m_relation",
                text:"关系维护",
                leaf:true
            }
            {
                id:"m_dynamic",
                text:"动态数据定义",
                leaf:true
            }
        ]
