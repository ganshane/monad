Ext.ns 'mg.Bootstrap'

Ext.apply mg.Bootstrap,
    initApplication:(app_path)->
        Ext.onReady ->
          Ext.get('loading-msg').update('Loading Application ...');
          app= Ext.application
            name: 'mg',
            appFolder: app_path
            renderTo: Ext.get('viewport')
            controllers: ['RelationController','ResourcesController',
                           'MainController','DynamicController']
            launch: ->
                Ext.get('loading-msg').update('Loading UI ...');
                Ext.create("mg.view.Main");
                hideMask = ->
                    Ext.get('loading').remove();
                    Ext.fly('loading-mask').animate({
                        opacity:0,
                        remove:true
                    });

                Ext.defer(hideMask, 250);
