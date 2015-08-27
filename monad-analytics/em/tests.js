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

  QUnit.test( "query", function( assert ) {
    assert.expect( 2 );
    var done = assert.async();
    mini_query('id:[4321 TO 4350]',function(r){
      assert.equal(30,r.count)
      assert.equal(1,Module.ContainerSize())
      Module.clearAllCollection();
      done();
    })
  });

  QUnit.test( "inPlaceAnd", function( assert ) {
    assert.expect( 2 );
    var done = assert.async();
    mini_query('id:[4321 TO 4350]',function(r){
      assert.equal(30,r.count)
      mini_query("id:[4341 TO 4360]",function(r2){
        Module.inPlaceAnd([r.key,r2.key],++key,function(r3){
          assert.equal(r3.count,10)
          Module.clearAllCollection();
          done();
        },onFail,onProgress)
      });
    })
  });

  QUnit.test( "inPlaceAndTop", function( assert ) {
    assert.expect( 2 );
    var json = {i:resource,q:'test'}
    var done = assert.async();

    mini_query('test',function(r){
      assert.equal(100000,r.count)
      mini_query("id:[4321 TO 4330]",function(r2){
        Module.inPlaceAndTop([r.key,r2.key],++key,function(r3){
          assert.equal(r3.count,10)
          Module.clearAllCollection();
          done();
        },2,onFail,onProgress)
      });
    })
  });

  QUnit.test( "andNot", function( assert ) {
    assert.expect( 3 );
    var json = {i:resource,q:'test'}
    var done = assert.async();

    mini_query('test',function(r){
      assert.equal(100000,r.count)
      mini_query("id:[4321 TO 4330]",function(r2){
      assert.equal(r2.count,10)
        Module.andNot([r.key,r2.key],++key,function(r3){
          assert.equal(r3.count,99990)
          Module.clearAllCollection();
          done();
        },onFail,onProgress)

      });
    })
  });



}

