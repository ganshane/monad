// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.model

/**
 *
 * @author jcai
 * @version 0.1
 */

class SearchResultTest {
  /*
  @Test
  def test_facet{
      val shardResult = new ShardResult
      shardResult.totalRecord = 9
      shardResult.facetArr = Array(
          "1234567890",
          "1234567891",
          "1234567892",
          "1234567893",
          "1234567894",
          "1234567895",
          "1234567896",
          "1234567897",
          "1234567898"
      )
      shardResult.results=Array(
          (new ObjectId("1e70316c9a7f18ecebaee2c0").toByteArray,2),
          (new ObjectId("1e70316c9a7f18ecebaee2c1").toByteArray,2),
          (new ObjectId("1e70316c9a7f18ecebaee2c2").toByteArray,1),
          (new ObjectId("1e70316c9a7f18ecebaee2c3").toByteArray,1),
          (new ObjectId("1e70316c9a7f18ecebaee2c4").toByteArray,1),
          (new ObjectId("1e70316c9a7f18ecebaee2c5").toByteArray,1),
          (new ObjectId("1e70316c9a7f18ecebaee2c6").toByteArray,1),
          (new ObjectId("1e70316c9a7f18ecebaee2c7").toByteArray,1),
          (new ObjectId("1e70316c9a7f18ecebaee2c8").toByteArray,1)
      )

      val result = SearchResult.mergeFacet("sfzh",List(shardResult))
      result.hits.foreach{x=>
          println(x.toString)
      }
      Assert.assertEquals(9,result.hitCount)
      Assert.assertEquals("1e70316c9a7f18ecebaee2c0",result.hits(0).toString)

  }
  @Test
  def test_result{
      val shardResult1= new ShardResult
      shardResult1.totalRecord = 101
      shardResult1.results=Array(
              (new ObjectId("1e70316c9a7f18ecebaee2c0").toByteArray,2.0f),
              (new ObjectId("1e70316c9a7f18ecebaee2c1").toByteArray,1.8f),
              (new ObjectId("1e70316c9a7f18ecebaee2c2").toByteArray,1.6f),
              (new ObjectId("1e70316c9a7f18ecebaee2c3").toByteArray,1.4f),
              (new ObjectId("1e70316c9a7f18ecebaee2c4").toByteArray,1.2f),
              (new ObjectId("1e70316c9a7f18ecebaee2c5").toByteArray,1.0f)
          )
      shardResult1.maxScore=2.0f

      val shardResult2= new ShardResult
      shardResult2.totalRecord = 301
      shardResult2.results=Array(
          (new ObjectId("2e70316c9a7f18ecebaee2d0").toByteArray,1.9f),
          (new ObjectId("2e70316c9a7f18ecebaee2d1").toByteArray,1.7f),
          (new ObjectId("2e70316c9a7f18ecebaee2d2").toByteArray,1.5f),
          (new ObjectId("2e70316c9a7f18ecebaee2d3").toByteArray,1.3f),
          (new ObjectId("2e70316c9a7f18ecebaee2d4").toByteArray,1.2f),
          (new ObjectId("2e70316c9a7f18ecebaee2d5").toByteArray,1.1f)
      )
      shardResult2.maxScore=1.9f

      var m=SearchResult.merge(0,5,List(shardResult1,shardResult2))
      Assert.assertEquals(402,m.hitCount)
      Assert.assertEquals(5,m.hits.size)
      //排在第四个的数字
      var objId  = m.hits(3)
      Assert.assertEquals("2e70316c9a7f18ecebaee2d1",objId.toString)

      m=SearchResult.merge(5,7,List(shardResult1,shardResult2))
      Assert.assertEquals(7,m.hits.size)
      objId  = m.hits(3)
      Assert.assertEquals("1e70316c9a7f18ecebaee2c4",objId.toString)
  }
  */
}
