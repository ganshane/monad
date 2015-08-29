(function( window ) {
var AnalyticsClient,
  config,
  hasOwn = Object.prototype.hasOwnProperty;
  AnalyticsClient = {};
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

extend(AnalyticsClient,{
  createCondition:function(){ return new Conditions();},
  /*
  top:function(callback,options){
    var _options = {category:Module.IdCategory.Person,top:100,offset:0}
    extend(_options,options)
     Module.top(_options.category,_options.key,_options.top,callback,_options.offset,config.fail,config.progress);
  },
  //args=[{i:xxx,q:'yyy",weight:zz}]+ callback=function(coll)
  query:function(parameters,callback){
    var op={weight:1}
    extend(op,parameters)
    //Module.query({i:op.i,q:op.q},++key,callback,config.fail,config.progress,op.weight);
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
  */
  //args=[Condition,Condition] callback=function(coll)
  inPlaceAndTopWithPositionMerged:function(args,callback,freq){
    var _freq = 1;
    if(freq != null)
      _freq = freq;

    var conditions=[];
    for(i = 0;i<args.length;i++){
      conditions[i]=args[i].toWorkerParameter();
    }
    current_task_callback = callback;
    worker.postMessage({op:OP_IN_PLACE_AND_TOP_WITH_POSITION_MERGED,parameters:{conditions:conditions,freq:freq}})
  },
  /*
  createBitSetWrapper:function(){
    var key_seq = ++ key;
    return {wrapper:Module.createBitSetWrapper(key_seq),key:key_seq};
  },
  */
  clearAllCollection:function(){
    worker.postMessage({op:OP_CLEAR_ALL_COLLECTION})
  },
  extend:extend
});

var current_task_callback = null
var worker = new Worker("analytics_worker.js")
worker.addEventListener("message",function(event) {
  switch(event.data.op){
    case OP_FAIL:
      onFail(event.data.message)
      break;
    case OP_PROGRESS:
      onProgress(event.data.message)
      break;
    default:
        current_task_callback(event.data.result)
      break;
  }
});

//DSL mode
function Conditions(){this.query_objects=[];}
Conditions.prototype = {
  op:'',
  freq:1,
  query:function(query_object){
    this.query_objects.push(query_object);
    this.op = OP_QUERY;
    return this;
  },
  top:function(callback,options){
    var top_func = function(coll){
      var _options = {key:coll.key}
      extend(_options,options)
      current_task_callback = callback;
      worker.postMessage({op:OP_TOP,parameters:_options})
    }
    this.execute(top_func)
  },
  execute:function(callback){
    current_task_callback = callback;
    switch(this.op){
      default:
        var message= {op:this.op,parameters:this.toWorkerParameter()};
        worker.postMessage(message);
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
  },
  toWorkerParameter:function(){
    return {query_objects:this.query_objects,freq:this.freq,op:this.op};
  }
}

AnalyticsClient.config = config;

window.Analytics = AnalyticsClient;

})((function(){return this;})());

