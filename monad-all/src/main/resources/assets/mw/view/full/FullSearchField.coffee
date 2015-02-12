# Copyright 2012 The EGF IT Software Department.
# site: http://www.ganshane.com

# 搜索的文本框组件
Ext.define 'mw.view.full.FullSearchField'
    extend: 'Ext.form.field.Trigger'
    alias: 'widget.FullSearchField'
    trigger1Cls: 'x-form-clear-trigger'
    trigger2Cls: 'x-form-search-trigger'

    hasSearch : false
    paramName : 'query'

    constructor: (config)->
        this.addEvents "search"
        this.callParent(arguments);
    initComponent: ->
        this.callParent(arguments);
        fun = (f, e) ->
                if e.getKey() is e.ENTER
                    this.onTrigger2Click();
        this.on 'specialkey',fun ,this
    afterRender: ->
        this.callParent();
        this.triggerCell.item(0).setDisplayed(false);

    onTrigger1Click : ->
        me = this;
        if (me.hasSearch)
            me.setValue('');
            me.hasSearch = false;
            me.triggerCell.item(0).setDisplayed(false);

    onTrigger2Click : ->
        me = this
        value = me.getValue()
        if (value.length < 1)
            me.onTrigger1Click();
            return;

        me.hasSearch = true;

        #trigger event
        resourceListMenu = Ext.getCmp('resourceListMenu')
        store = resourceListMenu.getStore();

        Ext.Array.each resourceListMenu.resources,(node)->
                 me.fireEvent "search",me,node,store

        me.triggerCell.item(0).setDisplayed(true);
        me.doComponentLayout();
