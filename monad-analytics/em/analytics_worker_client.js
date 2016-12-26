// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
(function( window ) {
var AnalyticsClient,
  config,
  hasOwn = Object.prototype.hasOwnProperty;
  /**
   * 全局使用的操作对象,必须使用 Analytics.init进行初始化.
   * @class Analytics
   * @static
   */
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
  current_task_callback:null,
  /**
   * 创建查询条件对象
   * @method createCondition 创建条件对象
   * @static
   * @return {Conditions} 条件集合对象
   */
  createCondition:function(){ return new Conditions(this.backendWorker);},
  performance:function(callback){
    this.current_task_callback = callback;
    this.backendWorker.postMessage({op:OP_PERFORMANCE});
  },
  /**
   * 初始化整个应用
   * @method init
   * @static
   * @param parameters 初始化参数.
   * @example
   *
   *     var parameters={
   *          fail:onFail,
   *          progress:onProgress,
   *          apiUrl:"http://localhost:9081/api",
   *          coreJsPath:"../build-em/em/monad_analytics.js"
   *      }
   *
   * @param parameters.coreJsPath 核心JS的路径
   * @param parameters.fail 当操作失败的回调函数，函数具备两个参数，分别是 code,message
   * @param parameters.progress 当操作进行中的回调函数，函数具备两个参数，分别是 code,message
   * @param parameters.apiUrl api服务器的路劲，譬如:http://localhost:9081/api
   *
   */
  init:function(parameters){
    config.fail = parameters.fail;
    config.progress = parameters.progress;
    var apiUrl = parameters.apiUrl;
    var coreJsPath = parameters.coreJsPath;
    this.backendWorker = new Worker(coreJsPath);
    var me=this;
    this.backendWorker.addEventListener("message",function(event) {
      switch(event.data.op){
        case OP_FAIL:
          if(parameters.fail)
            parameters.fail(event.data.code,event.data.message)
          else
            console.log("fail function is null")
          break;
        case OP_PROGRESS:
          if(parameters.progress)
            parameters.progress(event.data.code,event.data.message)
          else
            console.log("progress function is null")
          break;
        default:
          if(me.current_task_callback)
            me.current_task_callback(event.data.result)
          else
            console.log("callback is null!")

          me.current_task_callback = null

          break;
      }
    });
    this.backendWorker.postMessage({op:OP_INIT_URL,url:apiUrl});
  },
  /**
   * 清空所有的集合
   * @method clearAllCollection
   * @static
   */
  clearAllCollection:function(){
    this.backendWorker.postMessage({op:OP_CLEAR_ALL_COLLECTION})
  },
  /**
   * 清空某个集合，释放内存
   * @method clearCollection
   * @static
   * @param key 集合的key
   */
  clearCollection:function(key){
    this.backendWorker.postMessage({op:OP_CLEAR_COLLECTION,key:parseInt(key)})
  },
  extend:extend
});


//DSL mode
/**
* 条件对象,通常使用 Analytics.createCondition进行创建
* @class Conditions
* @constructor
* @private
*/
function Conditions(worker){
  this.worker = worker;
  this.query_objects=[];
}

Conditions.prototype = {
  op:'',
  freq:1,
  fullTextQuery:function(query_object){
    this.query_objects.push(query_object);
    this.op = OP_FULL_TEXT_QUERY;
    return this;
  },
  /**
   * 获得某一个集合对象
   * @method collection
   * @param collectionKey 集合的key
   * @returns {Conditions} 条件对象
   */
  collection:function(collectionKey){
    this.query_objects.push({key:parseInt(collectionKey)});
    this.op = OP_QUERY;
    return this;
  },
  /**
   * 设置一个查询条件，此条件将向后端服务器发生请求
   * @method query
   * @param {Object} query_object 查询使用的JSON
   *                 譬如： {i:'trace',q:'张三'} ,其中i为查询的索引/表，q为查询的语句
   * @param {Object} query_object.i 待查询的索引/表
   * @param {Object} query_object.q 查询语句
   * @return {Conditions} 查询使用的Conditions对象
   */
  query:function(query_object){
    this.query_objects.push(query_object);
    this.op = OP_QUERY;
    return this;
  },
  /**
   * 添加条件
   * @method addCondition
   * @param {Conditions} condition 另外一个条件对象
   * @return {Conditions} 查询使用的Conditions对象
   */
  addCondition:function(condition){
    this.query_objects.push(condition);
    return this;
  },
  /**
   * 取得一个集合的前N个数据
   * @method top
   * @param {Function} callback 回调函数,函数的参数是一个json数组，数组中的元素有:
   * @param {Int} callback.id 对象的id
   * @param {Int} callback.count 出现的次数
   * @param {Array} callback.p 位置信息,整数数组
   *
   * @param {Object} options 获取数据时候使用的参数
   * @param {Int} options.category 该集合的类别,默认是: Module.IdCategory.Person
   * @param {Int} options.top 取多少数据,默认是：100
   * @param {Int} options.offset 偏移量，默认是: 0
   * @return {Conditions} 查询使用的Conditions对象
   */
  top:function(callback,options){
    var top_func = (function(coll){
      var _options = {key:coll.key}
      extend(_options,options)
      AnalyticsClient.current_task_callback = callback;
      this.worker.postMessage({op:OP_TOP,parameters:_options})
    }).bind(this);
    this.execute(top_func)
  },
  /**
   * 执行操作，调用此方法才执行真正的操作(除top方法)
   * @method execute
   * @param {Function} callback 回调函数
   * @param {Int} callback.key 该集合的key
   * @param {Int} callback.count 集合的数量
   * @param {Int} callback.elapsed_time 执行该操作所需要的时间
   */
  execute:function(callback){
    AnalyticsClient.current_task_callback = callback;
    switch(this.op){
      default:
        var message= {op:this.op,parameters:this.toWorkerParameter()};
        this.worker.postMessage(message);
        break;
    }
  },
  /**
   * 设置and操作
   * @method inPlaceAnd
   * @return {Conditions} 查询使用的Conditions对象
   */
  inPlaceAnd:function(){
    this.op = OP_IN_PLACE_AND;
    return this;
  },
  /**
   * 设置andTop操作
   * @method inPlaceAndTop
   * @return {Conditions} 查询使用的Conditions对象
   */
  inPlaceAndTop:function(freq){
    this.op = OP_IN_PLACE_AND_TOP;
    this.freq = freq;
    return this;
  },
  /**
   * 设置inPlaceAndTopWithPositionMerged操作
   * @method inPlaceAndTopWithPositionMerged
   * @return {Conditions} 查询使用的Conditions对象
   */
  inPlaceAndTopWithPositionMerged:function(freq){
    this.op = OP_IN_PLACE_AND_TOP_WITH_POSITION_MERGED;
    this.freq = freq;
    return this;
  },
  /**
   * 设置inPlaceOr操作
   * @method inPlaceOr
   * @return {Conditions} 查询使用的Conditions对象
   */
  inPlaceOr:function(){
    this.op = OP_IN_PLACE_OR;
    return this;
  },
  /**
   * 设置andNot操作
   * @method andNot
   * @return {Conditions} 查询使用的Conditions对象
   */
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

