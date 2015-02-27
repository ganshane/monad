// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

/**
 *
 * @author jcai
 */
class SearchResultExtractorTest {
  /*
  @Test
  def test_DefaultDBObjectExtractor{
      val extractor = SearchResultExtractor.DefaultDBObjectExtractor
      val resource = XmlLoader.parseXML[ResourceDefinition](getClass.getResourceAsStream("/czrk.xml"),None)
      val dbObj = new BasicDBObject()
      dbObj.put(MonadFaceConstants.OBJECT_ID_FIELD_NAME,new ObjectId(new Date()))
      dbObj.put("sfzh","12345")
      val r = extractor.extract(resource,dbObj)
      Assert.assertTrue(r.get.has("sfzh"))
      Assert.assertEquals("12345",r.get.get("sfzh"))
  }
  @Test
  def test_DynamicDBObjectExtractor{
      val extractor = SearchResultExtractor.DynamicDBObjectExtractor
      val resource = XmlLoader.parseXML[ResourceDefinition](getClass.getResourceAsStream("/czrk.xml"),None)
      resource.dynamicType.descFormat="{sfzh}是{create_time}"
      val dbObj = new BasicDBObject()
      dbObj.put(MonadFaceConstants.OBJECT_ID_FIELD_NAME,new ObjectId(new Date()))
      dbObj.put("sfzh","12345")
      val r = extractor.extract(resource,dbObj)
      Assert.assertTrue(r.get.has("zjhm"))
      Assert.assertEquals("12345",r.get.get("zjhm"))
      Assert.assertEquals("12345是",r.get.get("_desc"))
  }
  @Test
  def test_extract {
      val resource = XmlLoader.parseXML[ResourceDefinition](getClass.getResourceAsStream("/czrk.xml"),None)
      val objectId = new ObjectId(new Date)

      val dfsOperator = EasyMock.createMock(classOf[DfsOperator])
      val apiConfig = new MonadApiConfig

      val dbObject = new BasicDBObject
      dbObject.put(MonadFaceConstants.OBJECT_ID_FIELD_NAME,objectId)
      dbObject.put("xm","test")
      dbObject.put("xb","1")
      dbObject.put("create_time",new Date().getTime)
      EasyMock.expect(dfsOperator.getData(resource,objectId)).andReturn(dbObject).times(2)

      EasyMock.replay(dfsOperator)

      val extractor = new SearchResultExtractor(dfsOperator, apiConfig)
      val searchRequest = new SearchRequest
      searchRequest.resourceName = "czrk"
      searchRequest.resource = resource
      searchRequest.q = "test"
      searchRequest.start = 10
      searchRequest.offset = 100

      var result = extractor.extract(searchRequest, sr => {
          new SearchResult(1000, Array[Array[Byte]](objectId.toByteArray),Some(Array[Int](123)))
      })
      Assert.assertEquals(1000,result.getInt("total"))
      val data = result.getJSONArray("data")
      Assert.assertEquals("test",data.getJSONObject(0).getString("xm"))
      Assert.assertEquals(123,data.getJSONObject(0).getInt("_count"))

      searchRequest.includeData = false
      result = extractor.extract(searchRequest, sr => {
          new SearchResult(1000, Array[Array[Byte]](objectId.toByteArray))
      })
      Assert.assertFalse(result.has("data"))


      //查询某一条具体数据
      searchRequest.objectId = objectId.toByteArray
      result = extractor.extract(searchRequest, sr => {
          Assert.fail("不应该被调用!")
          null
      })
      Assert.assertTrue(result.has("data"))
      Assert.assertEquals(1,result.getInt("total"))

      EasyMock.verify(dfsOperator)
  }
  */
}
