# mw application
Ext.ns 'mw.Bootstrap'
Ext.apply mw.Bootstrap,
    start: (appFolder,version) ->
      Ext.onReady ->
        Ext.application
            name: 'mw'
            appFolder: appFolder
            renderTo: Ext.get('viewport')
            controllers: ['MainController','FullSearchController']
            launch: ->
                Ext.get('loading-msg').update("Loading main ui ....")
                Ext.create "mw.view.Main",
                    monadVersion:version
                    renderTo:Ext.get("viewport")
                hideMask = ->
                    Ext.get('loading').remove();
                    Ext.fly('loading-mask').animate
                        opacity:0,
                        remove:true

                Ext.defer(hideMask, 250);

