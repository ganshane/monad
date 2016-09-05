// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
function analytics_onready(){
  Module.SetApiUrl("http://keyten:9081/api");
  console.log("loaded ....")
}
var Module = {
    filePackagePrefixURL: "../build-em/em/",
    memoryInitializerPrefixURL: "../build-em/em/"
}

importScripts("async.min.js")
importScripts('../build-em/em/monad_analytics.js');
importScripts('analytics.js');
importScripts('ops.js');

Analytics.config.fail = function(fail_message){
  postMessage({op:OP_FAIL,message:fail_message})
}
Analytics.config.progress= function(progress_message){
  postMessage({op:OP_PROGRESS,message:progress_message})
}


onmessage=function(event){
  var op = event.data.op;
  var client_callback = function(result){
     postMessage({op:op,result:result})
  };
  switch(op){
    case OP_CLEAR_ALL_COLLECTION:
      Analytics.clearAllCollection();
      break;
    case OP_TOP:
      Analytics.top(client_callback,event.data.parameters)
      break;
    case OP_IN_PLACE_AND_TOP_WITH_POSITION_MERGED:
      var parameters = event.data.parameters;
      var conditions = parameters.query_objects;
      var freq = parameters.freq;

      var condition_objects = [];
      for(i =0;i<conditions.length;i++){
        var condition = Analytics.createCondition();
        Analytics.extend(condition,conditions[i])
        condition_objects[i]=condition;
      }

      Analytics.inPlaceAndTopWithPositionMerged(condition_objects,client_callback,freq)

      break;
    case OP_PERFORMANCE:
        performance(client_callback)
        break;
    default:
      var condition = Analytics.createCondition();
      var parameters = event.data.parameters;
      Analytics.extend(condition,parameters)

      condition.execute(client_callback);
  }
}


function performance(callback){
  Analytics.config.progress("creating wrapper ...")
  var wrapper_object = Analytics.createBitSetWrapper();
  var bit_set_wrapper = wrapper_object.wrapper;
  for(j=0;j<50;j++){
    bit_set_wrapper.NewSeg(j,2000000)
    for(i=0;i<200000;i++ ){
      bit_set_wrapper.FastSet(i*3);
    }
  }
  bit_set_wrapper.Commit();

  Analytics.config.progress("wrapper created");
  //assert.ok(bit_set_wrapper.FastGet(100))
  /*
  var wrapper_object2 = Analytics.createBitSetWrapper();
  var bit_set_wrapper2 = wrapper_object2.wrapper;
  bit_set_wrapper2.NewSeg(1,100000000)
  for(i=0;i<2;i++ ){
    bit_set_wrapper2.FastSet(i);
  }
  bit_set_wrapper2.Commit();
  */

  Analytics.inPlaceAndTop([
    wrapper_object.key,
    wrapper_object.key
    ],callback,1);

  Analytics.config.progress("end inPlace And");
}
