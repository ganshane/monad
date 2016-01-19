// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.id.internal

import java.io.File

import monad.id.config.MonadIdConfig
import monad.jni.services.JniLoader
import monad.protocol.internal.InternalIdProto.IdCategory
import org.junit.{Assert, Before, Test}

/**
 * implements id service
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
class IdServiceImplTest {
  @Before
  def loadJni() {
    val file = new File("support")
    if (file.exists())
      JniLoader.loadJniLibrary("support", "stderr")
    else
      JniLoader.loadJniLibrary("../support", "stderr")
  }
  @Test
  def test_id_service: Unit ={
    val config = new MonadIdConfig
    config.id.noSql.path="target/ids"
    val dir = new File(config.id.noSql.path)
    //FileUtils.deleteQuietly(dir)
    //FileUtils.forceMkdir(dir)

    val idService = new IdServiceImpl(config)
    idService.initService(null)

    val mask = (1 << 16) - 1
    val start = System.currentTimeMillis()
    0 until 10000000 foreach { i =>
      if((i & mask) == 0 && i > 0){
        val end = System.currentTimeMillis()
        println(i+ "qps:"+(i*1000.0)/(end-start))
      }
      val person = "413028198009121514"+i
      val ordOpt = idService.getOrAddId(IdCategory.Person, person)
      Assert.assertTrue(ordOpt.isDefined)
      Assert.assertEquals(person, idService.getIdLabel(IdCategory.Person, ordOpt.get).get)
    }

    idService.shutdown()
  }
}
