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
    Analytics.createCondition().query({i:resource,q:'id:[4321 TO 4350]',weight:2}).top(function(r){
        assert.equal(r.length,30)
        done();
      })
  });

/*
  QUnit.test( "inPlaceAnd", function( assert ) {
    assert.expect( 1 );
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
  QUnit.test( "inPlaceAndTopWithPositionMerged", function( assert ) {
    var done = assert.async();
    var condition1 = Analytics.createCondition()
                       .query({i:resource,q:'test'})
                       .query({i:resource,q:'id:[4330 TO 4370]'})
                       .inPlaceAndTop(2);
    var condition2 = Analytics.createCondition()
                       .query({i:resource,q:'id:[4341 TO 4360]'})
                       .inPlaceAndTop(1);

    Analytics.inPlaceAndTopWithPositionMerged([condition1,condition2],
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

  QUnit.test( "dsl", function( assert ) {
    var done = assert.async();
    var condition = Analytics.createCondition()
      .query({i:resource,q:'test'})
      .query({i:resource,q:'id:[4321 TO 4330]'})
      .inPlaceAnd().top(function(objects,key){
        assert.equal(10,objects.length)
        done();
      });
  });
  /*
  QUnit.test( "Performance", function( assert ) {

    assert.expect( 1 );
    var done = assert.async();

    onMessage("creating wrapper ...")
    var wrapper_object = Analytics.createBitSetWrapper();
    var bit_set_wrapper = wrapper_object.wrapper;
    bit_set_wrapper.NewSeg(1,100000000)
    for(i=0;i<10000000;i++ ){
      bit_set_wrapper.FastSet(i*3);
    }
    bit_set_wrapper.Commit();

    onMessage("wrapper created");
    //assert.ok(bit_set_wrapper.FastGet(100))

    Analytics.inPlaceAnd([
      wrapper_object.key,
      wrapper_object.key,
      wrapper_object.key,
      ],function(result){
          assert.equal(result.count,10000000)
          Analytics.clearAllCollection();
          done();
    });
  });

*/

