###
 Copyright 2012 The EGF IT Software Department.
 site: http://www.ganshane.com
###

Ext.define 'mg.model.Resource',
    extend: 'Ext.data.Model',
    idProperty: 'name',
    fields: [{name:'name',type:'string'}, 'cnName','quantity','maxValue']
