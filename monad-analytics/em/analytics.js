// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
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
  if(hasOwn.call(query_object,"query_objects")){//Condition
    console.log("condition!!")
    iterator_callback(null,function(query_callback){
      query_object.execute(function(coll){query_callback(null,coll)})
    })
  }else if(is_json(query_object)){
    iterator_callback(null,function(query_callback){
      Analytics.query(query_object,function(coll){query_callback(null,coll)});
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
  		if(err){
  	   config.fail(err);
  	   return;
  	  }
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
  createCondition:function(){ return new Conditions();},
  top:function(callback,options){
    var _options = {category:Module.IdCategory.Person,top:100,offset:0}
    extend(_options,options)
     Module.top(_options.category,_options.key,_options.top,callback,_options.offset,config.fail,config.progress);
  },
  //args=[{i:xxx,q:'yyy",weight:zz}]+ callback=function(coll)
  fullTextQuery:function(parameters,callback){
    var op={weight:1}
    extend(op,parameters)
    Module.fullTextQuery({i:op.i,q:op.q},callback,config.fail,config.progress);
  },
  //args=[{i:xxx,q:'yyy",weight:zz}]+ callback=function(coll)
  query:function(parameters,callback){
    var op={weight:1}
    extend(op,parameters)
    Module.query({i:op.i,q:op.q},++key,callback,config.fail,config.progress,op.weight);
  },
  //args=[Condition|{i:xxx,q:'xxx"}|key_id]+ callback=function(coll)
  inPlaceAndTop:function(args,callback,freq){
    var _freq = 1;
    if(freq != null)
      _freq = freq;

  	base_operation_func(args,function(keys){
      Module.inPlaceAndTop(keys,++key,callback,_freq,config.fail,config.progress)
    });
  },
  //args=[Condition|{i:xxx,q:'xxx"}|key_id]+ callback=function(coll)
  andNot:function(args,callback){
  	base_operation_func(args,function(keys){
      Module.andNot(keys,++key,callback,config.fail,config.progress)
    });
  },
  //args=[Condition|{i:xxx,q:'xxx"}|key_id]+ callback=function(coll)
  inPlaceAnd:function(args,callback){
  	base_operation_func(args,function(keys){
      Module.inPlaceAnd(keys,++key,callback,config.fail,config.progress)
    });
  },
  //args=[Condition|{i:xxx,q:'xxx"}|key_id]+ callback=function(coll)
  inPlaceOr:function(args,callback){
  	base_operation_func(args,function(keys){
      Module.inPlaceOr(keys,++key,callback,config.fail,config.progress)
    });
  },
  //args=[Condition,Condition] callback=function(coll)
  inPlaceAndTopWithPositionMerged:function(args,callback,freq){
    var _freq = 1;
    if(freq != null)
      _freq = freq;

    async.map(args,function(query_object,iterator_callback){
          query_object.execute(function(coll){iterator_callback(null,coll)})
      },function(err,task_results){
  		  if(err){
  	      config.fail(err);
  	      return;
  	    }
  			var keys = [];
  			for(var i=0;i<task_results.length;i++){
  				keys[i] = task_results[i].key;
  			}
        Module.inPlaceAndTopWithPositionMerged(keys,++key,function(coll){callback(coll);},_freq,config.fail,config.progress)
    });
  },
  createBitSetWrapper:function(){
    var key_seq = ++ key;
    return {wrapper:Module.createBitSetWrapper(key_seq),key:key_seq};
  },
  clearAllCollection:function(){
    Module.clearAllCollection();
  },
  extend:extend
});

//DSL mode
function Conditions(){this.query_objects=[];}
Conditions.prototype = {
  op:0,
  freq:1,
  query:function(query_object){
    this.query_objects.push(query_object);
    return this;
  },
  top:function(callback,options){
    var top_func = function(coll){
      var _options = {category:Module.IdCategory.Person,top:100,offset:0,key:coll.key}
      extend(_options,options)
      Analytics.top(callback,_options)
    }
    this.execute(top_func)
  },
  execute:function(callback){
    switch(this.op){
      case(OP_IN_PLACE_AND):
        Analytics.inPlaceAnd(this.query_objects,callback);
        break;
      case(OP_IN_PLACE_AND_TOP):
        Analytics.inPlaceAndTop(this.query_objects,callback,this.freq);
        break;
      case(OP_IN_PLACE_OR):
        Analytics.inPlaceOr(this.query_objects,callback);
        break;
      case(OP_AND_NOT):
        Analytics.andNot(this.query_objects,callback);
        break;
      case (OP_IN_PLACE_AND_TOP_WITH_POSITION_MERGED):
        Analytics.inPlaceAndTopWithPositionMerged(this.query_objects,callback,this.freq);
        break
      case (OP_QUERY):
        Analytics.query(this.query_objects[0],callback)
        break;
      case (OP_FULL_TEXT_QUERY):
        Analytics.fullTextQuery(this.query_objects[0],callback)
        break;
      default:
        config.fail("op ["+this.op+"] unrecognized!")
        break;
    }
  },
  inPlaceAnd:function(){
    this.op = OP_IN_PLACE_AND;
    return this;
  },
  inPlaceAndTop:function(freq){
    this.op = OP_IN_PLACE_AND_TOP;
    this.freq = freq;
    return this;
  },
  inPlaceAndTopWithPositionMerged:function(freq){
    this.op = OP_IN_PLACE_AND_TOP_WITH_POSITION_MERGED;
    this.freq = freq;
    return this;
  },
  inPlaceOr:function(callback){
    this.op = OP_IN_PLACE_OR;
    return this;
  },
  andNot:function(callback){
    this.op = OP_AND_NOT;
    return this;
  }
}

Analytics.config = config;

window.Analytics = Analytics;

})((function(){return this;})());


