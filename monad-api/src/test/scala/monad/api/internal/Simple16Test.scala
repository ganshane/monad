// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal

import org.junit.Test

/**
 *
 * @author jcai
 */
class Simple16Test {
  @Test
  def test_compress {
    val in = Array(1, 3, 6, 9, 100, 200, 300, 1024, 2048, 4096)
    val out = new Array[Int](in.length)
    var num = Simple16.s16Compress(out, 0, in, 0, 10)
    println(num)
    println(out.toList)
    num = Simple16.s16Compress(out, 1, in, 4, 10)
    println(num)
    println(out.toList)
  }
}
