// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.internal

import java.util.regex.Pattern

import org.junit.Test

import scala.collection.mutable

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-09-06
 */
class HttpResetClientTest {
  private val pattern = Pattern.compile("channel=([0-9a-z]+)")
  private val finalUrl = "http://gsactivity.diditaxi.com.cn/gulfstream/activity/v2/giftpackage/index?channel=%s"
  @Test
  def check_url(): Unit ={
    val range1 = 0 to 9
    val range2 = 'A'.toInt to 'Z'.toInt
    val range3 = 'a'.toInt to 'z'.toInt

    val buffer = mutable.ListBuffer[Int]()
    buffer.appendAll(range1)
    buffer.appendAll(range2)
    buffer.appendAll(range3)
    val list = buffer.map{
      case x:Int if x >=0 && x <= 9 =>
        x.toString.charAt(0)
      case other =>
        other.toChar
    }.toList
    //list.foreach(println)

    val urlStream = list.toStream
      .flatMap(x=>list.toStream.map{y=>x+""+y})
      .flatMap(x=>list.toStream.map{y=>x+y})
      .flatMap(x=>list.toStream.map{y=>x+y})
    urlStream.par.foreach{ u =>
      val client = new  HttpRestClientImpl()
      val url ="http://dc.tt/%s".format(u)
      val str = client.get(url)
      val matcher = pattern.matcher(str)
      if(matcher.find()){
        println(url)
        //val url = finalUrl.format(matcher.group(1))
        //println(client.get(url))
      }
    }
  }
}
