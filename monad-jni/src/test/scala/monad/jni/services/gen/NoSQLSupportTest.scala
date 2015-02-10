// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.jni.services.gen

import java.io.File

import monad.support.MonadSupportConstants
import org.apache.commons.io.FileUtils
import org.junit.{Assert, Test}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-09
 */
class NoSQLSupportTest extends BaseJniTestCase{
  @Test
  def test_jni: Unit ={
    val dir = "target/nosql_support_test"
    FileUtils.deleteDirectory(new File(dir))
    FileUtils.forceMkdir(new File(dir))
    val options = new NoSQLOptions()
    val support = new NoSQLSupport("target/nosql_support_test",options)
    val testKey = "Test-Key"
    val testValue = "Test-Value"
    val status = support.RawPut(testKey.getBytes(MonadSupportConstants.UTF8_ENCODING),
      testValue.getBytes(MonadSupportConstants.UTF8_ENCODING)
    )
    Assert.assertTrue(status.ok())
    val value = support.Get(testKey.getBytes(MonadSupportConstants.UTF8_ENCODING))
    Assert.assertEquals(testValue,new String(value))

    support.delete()
  }
}
