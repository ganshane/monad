// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
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
      var el = document.createElement("div");
      el.setAttribute("style","color:red")
      el.appendChild(document.createTextNode(msg));
      document.getElementById("msg").appendChild(el);
  }
  function onProgress(msg){
      onMessage(msg);
  }
  Analytics.config.fail = onFail;
  Analytics.config.progress= onProgress;

  QUnit.test( "fullTextQuery", function( assert ) {
      var done = assert.async();
      Analytics.createCondition().fullTextQuery({i:resource,q:'id:[4321 TO 4350]'}).execute(function(r){
          //alert(r);
          var json = eval("("+r+")");
          assert.equal(100000, json["all"]);
          onMessage("query result:"+r)
          done();
      })
  });

  QUnit.test( "query", function( assert ) {

    var done = assert.async();
    Analytics.createCondition().query({i:resource,q:'id:[4321 TO 4350]',weight:2}).top(function(r){
        assert.equal(r.length,30)
        done();
      })
  });

  QUnit.test( "inPlaceAnd", function( assert ) {
    assert.expect( 1 );
    var done = assert.async();
    Analytics.createCondition()
      .query({i:resource,q:'id:[4321 TO 4350]'})
      .query({i:resource,q:'id:[4341 TO 4360]'})
      .inPlaceAnd().execute(function(result){
          assert.equal(result.count,10)
          done();
      })
  });

  QUnit.test( "inPlaceOr", function( assert ) {
    var done = assert.async();
    Analytics.createCondition()
      .query({i:resource,q:'id:[4321 TO 4350]'})
      .query({i:resource,q:'id:[4341 TO 4360]'})
      .inPlaceOr().execute(function(result){
          assert.equal(result.count,40)
          done();
    });

  });
  QUnit.test( "inPlaceAndTop", function( assert ) {
    var done = assert.async();

    Analytics.createCondition()
      .query({i:resource,q:'test'})
      .query({i:resource,q:'id:[4341 TO 4360]'})
      .inPlaceAndTop(2)
      .execute(function(result){
        assert.equal(result.count,20)
        //Analytics.clearAllCollection();
        done();
      });
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

    Analytics.createCondition()
          .addCondition(condition1)
          .addCondition(condition2)
          .inPlaceAndTopWithPositionMerged(2)
          .execute(function(result){
              assert.equal(result.count,20)
              Analytics.clearAllCollection();
              done();
          });
  });

  QUnit.test( "andNot", function( assert ) {
    var done = assert.async();
    Analytics.createCondition()
      .query({i:resource,q:'test'})
      .query({i:resource,q:'id:[4321 TO 4330]'})
      .andNot().execute(function(r){
        assert.equal(r.count,99990)
        Analytics.clearAllCollection();
        done();
      });
  });

  QUnit.test( "performance", function( assert ) {
    var done = assert.async();
    Analytics.performance(function(r){
      assert.ok(r);
      Analytics.clearAllCollection();
      done();
    });
  });
