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
  Analytics.config.fail = onFail;
  Analytics.config.progress= onProgress;

  QUnit.test( "query", function( assert ) {
    var done = assert.async();
    Analytics.query(resource,'id:[4321 TO 4350]',function(r){
      assert.equal(30,r.count)
      assert.equal(1,Module.ContainerSize())

      Analytics.top(Module.IdCategory.Person,r.key,100,0,function(result,key){
        assert.equal(30,result.length)
        done();
      })

      //Analytics.clearAllCollection();
    })
  });

  QUnit.test( "inPlaceAnd", function( assert ) {
    var done = assert.async();
    Analytics.inPlaceAnd([
      {i:resource,q:'id:[4321 TO 4350]'},
      {i:resource,q:'id:[4341 TO 4360]'}
      ],function(result){
          assert.equal(result.count,10)
          Analytics.clearAllCollection();
          done();
    });
  });
  QUnit.test( "inPlaceOr", function( assert ) {
    var done = assert.async();
    Analytics.inPlaceOr([
      {i:resource,q:'id:[4321 TO 4350]'},
      {i:resource,q:'id:[4341 TO 4360]'}
      ],function(result){
          assert.equal(result.count,40)
          Analytics.clearAllCollection();
          done();
    });
  });

  QUnit.test( "inPlaceAndTop", function( assert ) {
    var done = assert.async();
    Analytics.inPlaceAndTop([{i:resource,q:'test'},{i:resource,q:'id:[4341 TO 4360]'}],
      function(result){
        assert.equal(result.count,20)
        Analytics.clearAllCollection();
        done();
    },2);
  });

  QUnit.test( "andNot", function( assert ) {
    var done = assert.async();
    Analytics.andNot([{i:resource,q:'test'},{i:resource,q:'id:[4321 TO 4330]'}],
      function(r){
        assert.equal(r.count,99990)
        Analytics.clearAllCollection();
        done();
    });
  });


