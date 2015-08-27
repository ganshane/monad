function wait_load_analytics(){
  if(!analytics_loaded){
    console.log("waiting....")
    setTimeout(wait_load_analytics,1000)
    return;
  }

  test_function();

}

wait_load_analytics();


function test_function(){
  var resource = "test_100000";
  var key = 0;

  function onMessage(str){
      var el = document.createElement("div");
      el.appendChild(document.createTextNode(str));
      document.getElementById("msg").appendChild(el);
  }
  function onData(obj){
      onMessage("分析结果数:"+obj.count);
  }
  function onFail(msg){
      onMessage(msg);
  }
  function onProgress(msg){
      onMessage(msg);
  }
  function mini_query(query,fun){
    Module.query({i:resource,q:query},++key,fun,onFail,onProgress,1);
  }
  function mini_query2(query,callback){
    Module.query({i:resource,q:query},++key,function(r){callback(null,r);},onFail,onProgress,1);
  }
  function async_query(query){
    return function(callback){
      Module.query({i:resource,q:query},++key,function(r){callback(null,r);},onFail,onProgress,1);
    }
  }
  Analytics.config.fail = onFail;
  Analytics.config.progress= onProgress;

  QUnit.test( "query", function( assert ) {
    assert.expect( 2 );
    var done = assert.async();
    Analytics.query(resource,'id:[4321 TO 4350]',function(r){
      assert.equal(30,r.count)
      assert.equal(1,Module.ContainerSize())
      Module.clearAllCollection();
      done();
    })
  });

  QUnit.test( "inPlaceAnd", function( assert ) {
    var done = assert.async();
    Analytics.inPlaceAnd([
      {i:resource,q:'id:[4321 TO 4350]'},
      {i:resource,q:'id:[4341 TO 4360]'}
      ],function(result){
          assert.equal(result.count,10)
          done();
    });
  });

  QUnit.test( "inPlaceAndTop", function( assert ) {
    var done = assert.async();
    var done = assert.async();
    Analytics.inPlaceAndTop([
      {i:resource,q:'test'},
      {i:resource,q:'id:[4341 TO 4360]'}
      ],function(result){
          assert.equal(result.count,20)
          done();
    },2);

  });


  QUnit.test( "andNot", function( assert ) {
    var done = assert.async();
    async.parallel([
      async_query('test'),
      async_query('id:[4321 TO 4330]')
    ],function(err,results){
        assert.notOk(err)
        assert.equal(results.length,2)
        Module.andNot([results[0].key,results[1].key],++key,function(r){
          assert.equal(r.count,99990)
          Module.clearAllCollection();
          done();
        },onFail,onProgress)
      });
    });




}

