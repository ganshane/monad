// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import org.junit.{Assert, Test}

/**
 * Created by jcai on 14-8-17.
 */
class LocalSimpleStoreTest {
  @Test
  def test_version() {
    val store = new LocalSimpleStore("target/test.local")
    for (v <- 0 until 100) {
      store.put("asdf", "fdsa")
      store.put("sdf", "fdsa")
      store.put("df", "fdsa")
      store.put("a" + v, v)
    }
    Assert.assertEquals("fdsa", store.get("asdf").get)

  }
}
