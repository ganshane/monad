(function( window ) {
var Analytics,
  config,
  hasOwn = Object.prototype.hasOwnProperty;
Analytics = {};
config = {
 fail:function(message){},
 progress:function(message){}
};

function extend( a, b, undefOnly ) {
	for ( var prop in b ) {
		if ( hasOwn.call( b, prop ) ) {

			// Avoid "Member not found" error in IE8 caused by messing with window.constructor
			if ( !( prop === "constructor" && a === window ) ) {
				if ( b[ prop ] === undefined ) {
					delete a[ prop ];
				} else if ( !( undefOnly && typeof a[ prop ] !== "undefined" ) ) {
					a[ prop ] = b[ prop ];
				}
			}
		}
	}

	return a;
}

//自增长序列
var key = 0;

extend(Analytics,{
  query:function(index,q1,callback){
  	var weight = 1;
  	if(arguments.length == 4){
  		weight = arguments[3];
  	}
    Module.query({i:index,q:q1},++key,callback,config.fail,config.progress,weight);
  },
  inPlaceAndTop:function(args,callback){
  	var freq = 1;
  	if(arguments.length == 3)
  		freq = arguments[2]

  	var weight = 1;
  	if(arguments.length == 4)
  		weight = arguments[3];

  	async.map(args,function(queryObj,callback){
  		callback(null,function(query_callback){
  			return Analytics.query(queryObj.i,queryObj.q,function(coll){
  				query_callback(null,coll)}
  			);
  		});
  	},function(err,results){
  		if(err)
  	     config.fail(err)
  		//console.log(results.length)
  		async.parallel(results,function(err,task_results){
  			var keys = [];
  			for(var i=0;i<task_results.length;i++){
  				keys[i] = task_results[i].key;
  			}
  			//console.log(keys)
        Module.inPlaceAndTop(keys,++key,
          function(coll){
            callback(coll);
            Module.clearAllCollection();
        	},freq,config.fail,config.progress)
        });
  	})
  },
  inPlaceAnd:function(args,callback){
  	async.map(args,function(queryObj,callback){

  		callback(null,function(query_callback){
  			return Analytics.query(queryObj.i,queryObj.q,function(coll){
  				query_callback(null,coll)}
  			);
  		});
  	},function(err,results){
  		if(err)
  	     config.fail(err)
  		//console.log(results.length)
  		async.parallel(results,function(err,task_results){
  			var keys = [];
  			for(var i=0;i<task_results.length;i++){
  				keys[i] = task_results[i].key;
  			}
  			//console.log(keys)
        Module.inPlaceAnd(keys,++key,
          function(coll){
            callback(coll);
            Module.clearAllCollection();
        	},config.fail,config.progress)

        },config.fail,config.progress);
  		})
  	}
  });

Analytics.config = config;

window.Analytics = Analytics;

})((function(){return this;})());
