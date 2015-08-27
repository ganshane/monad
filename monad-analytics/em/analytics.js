(function( window ) {
var Analytics,
  config,
  hasOwn = Object.prototype.hasOwnProperty;
Analytics = {};
config = {
 fail:function(message){},
 progress:function(message){}
};
//等待Module进行初始化完毕
function wait_load_analytics(){
  if(!window.analytics_loaded){
    console.log("waiting....")
    setTimeout(wait_load_analytics,1000)
    return;
  }
}

wait_load_analytics();

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
//判断是否位json对象
function is_json(obj){
  return typeof(obj) == "object" && Object.prototype.toString.call(obj).toLowerCase() == "[object object]" && !obj.length;
}
//创建迭代使用的函数
function createQueryFunction(query_object,iterator_callback){
  if(is_json(query_object)){
    iterator_callback(null,function(query_callback){
      Analytics.query(query_object.i,query_object.q,function(coll){query_callback(null,coll)});
    });
  }else{
    iterator_callback(null,function(query_callback){
      query_callback(null,Module.getCollectionProperties(query_object));
    })
  }
}
//异步执行
function base_operation_func(query_parameters,operation_func){
  	async.map(query_parameters,createQueryFunction,function(err,results){
  		if(err)
  	     config.fail(err)
  		async.parallel(results,function(err,task_results){
  			var keys = [];
  			for(var i=0;i<task_results.length;i++){
  				keys[i] = task_results[i].key;
  			}
  			operation_func(keys)
  		})
  	})
}
//自增长序列
var key = 0;

extend(Analytics,{
  top:function(category,key,topN,offset,callback){
     Module.top(category,key,topN,callback,offset,config.fail,config.progress);
  },
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

  	base_operation_func(args,function(keys){
      Module.inPlaceAndTop(keys,++key,function(coll){callback(coll);},freq,config.fail,config.progress)
    });
  },
  andNot:function(args,callback){
  	base_operation_func(args,function(keys){
      Module.andNot(keys,++key,function(coll){callback(coll);},config.fail,config.progress)
    });
  },
  inPlaceAnd:function(args,callback){
  	base_operation_func(args,function(keys){
      Module.inPlaceAnd(keys,++key,function(coll){callback(coll);},config.fail,config.progress)
    });
  },
  inPlaceOr:function(args,callback){
  	base_operation_func(args,function(keys){
      Module.inPlaceOr(keys,++key,function(coll){callback(coll);},config.fail,config.progress)
    });
  },
  clearAllCollection:function(){
    Module.clearAllCollection();
  }
});

Analytics.config = config;

window.Analytics = Analytics;

})((function(){return this;})());
