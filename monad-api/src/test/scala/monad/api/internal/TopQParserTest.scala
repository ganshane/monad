package monad.api.internal

import org.junit.Test

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2016-09-09
  */
class TopQParserTest {
  private val pattern = "([\\d]+)\\@([\\d]+)".r
  @Test
  def test_parse: Unit ={
    val q = "123@12,123@321,543@1,"
    val groups = pattern.findAllMatchIn(q)
    while(groups.hasNext){
      val g = groups.next()
      println(g.group(1),g.group(2))
    }

  }
}
