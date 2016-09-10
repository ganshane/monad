// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
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
  backendWorker:{},
  createCondition:function(){ return new Conditions(this.backendWorker);},
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
  performance:function(callback){
    current_task_callback = callback;
    this.backendWorker.postMessage({op:OP_PERFORMANCE});
  },
  init:function(parameters){
    config.fail = parameters.fail;
    config.progress = parameters.progress;
    var apiUrl = parameters.apiUrl;
    var coreJsPath = parameters.coreJsPath;
    this.backendWorker = new Worker(coreJsPath);
    this.backendWorker.addEventListener("message",function(event) {
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
    this.backendWorker.postMessage({op:OP_INIT_URL,url:apiUrl});
  },
  clearAllCollection:function(){
    this.backendWorker.postMessage({op:OP_CLEAR_ALL_COLLECTION})
  },
  extend:extend
});

var current_task_callback = null

//DSL mode
function Conditions(worker){this.worker = worker; this.query_objects=[];}
Conditions.prototype = {
  op:'',
  freq:1,
  fullTextQuery:function(query_object){
    this.query_objects.push(query_object);
    this.op = OP_FULL_TEXT_QUERY;
    return this;
  },
  collection:function(collectionKey){
    this.query_objects.push({key:parseInt(collectionKey)});
    this.op = OP_QUERY;
    return this;
  },
  query:function(query_object){
    this.query_objects.push(query_object);
    this.op = OP_QUERY;
    return this;
  },
  addCondition:function(condition){
    this.query_objects.push(condition);
    return this;
  },
  top:function(callback,options){
    var top_func = function(coll){
      var _options = {key:coll.key}
      extend(_options,options)
      current_task_callback = callback;
      this.worker.postMessage({op:OP_TOP,parameters:_options})
    };
    this.execute(top_func)
  },
  execute:function(callback){
    current_task_callback = callback;
    switch(this.op){
      default:
        var message= {op:this.op,parameters:this.toWorkerParameter()};
        this.worker.postMessage(message);
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
  inPlaceOr:function(){
    this.op = OP_IN_PLACE_OR;
    return this;
  },
  andNot:function(){
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

