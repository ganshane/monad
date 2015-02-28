// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.internal

import java.io.{IOException, InputStream}
import java.net.URI

import monad.support.MonadSupportConstants
import monad.support.services.HttpRestClient
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{CloseableHttpResponse, RequestBuilder}
import org.apache.http.config.ConnectionConfig
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.util.EntityUtils

/**
 * http rest client
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-21
 */
object HttpRestClientInstance {
  final val httpClient: CloseableHttpClient = createHttpClient

  private def createHttpClient: CloseableHttpClient = {
    val defaultConfig: RequestConfig = RequestConfig.custom.setConnectTimeout(5 * 1000).setSocketTimeout(5 * 1000).build
    val defaultConnectionConfig = ConnectionConfig.custom().setCharset(MonadSupportConstants.UTF8_ENCODING_CHARSET).build()
    HttpClientBuilder.create.setDefaultRequestConfig(defaultConfig).setDefaultConnectionConfig(defaultConnectionConfig).setUserAgent("monad/1.0").build
  }

}

class HttpRestClientImpl extends HttpRestClient {
  /**
   * 通过get方式获取远端API内容
   * @param url 访问的URL
   * @param params 访问参数
   * @param headers http的头信息
   * @param encoding 请求和返回所使用的编码
   * @return API返回的字符串
   */
  override def get(url: String, params: Option[Map[String, String]], headers: Option[Map[String, String]], encoding: String): String = {
    try {
      val get = RequestBuilder.get()
        .setUri(new URI(url))

      params.foreach(m =>
        m.foreach { case (k, v) => get.addParameter(k, v)}
      )
      headers.foreach { m =>
        m.foreach { case (k, v) => get.addHeader(k, v)}
      }

      val response: CloseableHttpResponse = HttpRestClientInstance.httpClient.execute(get.build())
      val entity: HttpEntity = response.getEntity
      try {
        if (response.getStatusLine.getStatusCode == 200) {
          var stream: InputStream = null
          try {
            stream = entity.getContent
            IOUtils.toString(stream, encoding)
          } finally {
            IOUtils.closeQuietly(stream)
          }
        }
        else throw new RuntimeException(response.getStatusLine.toString)
      }
      finally {
        EntityUtils.consume(entity)
        IOUtils.closeQuietly(response)
      }
    }
    catch {
      case e: IOException => {
        throw new RuntimeException(e)
      }
    }
  }

  /**
   * 通过post方式获取远端API内容
   * @param url 远端URL地址
   * @param params 访问参数
   * @param headers http请求使用的头
   * @param encoding 请求和返回所使用的编码
   * @return API返回的字符串
   */
  override def post(url: String, params: Option[Map[String, String]], headers: Option[Map[String, String]], encoding: String): String = {
    try {
      val post = RequestBuilder.post()
        .setUri(new URI(url))

      params.foreach(m =>
        m.foreach { case (k, v) => post.addParameter(k, v)}
      )
      headers.foreach { m =>
        m.foreach { case (k, v) => post.addHeader(k, v)}
      }

      val response: CloseableHttpResponse = HttpRestClientInstance.httpClient.execute(post.build())
      val entity: HttpEntity = response.getEntity
      try {
        if (response.getStatusLine.getStatusCode == 200) {
          var stream: InputStream = null
          try {
            stream = entity.getContent
            IOUtils.toString(stream, encoding)
          } finally {
            IOUtils.closeQuietly(stream)
          }
        }
        else throw new RuntimeException(response.getStatusLine.toString)
      }
      finally {
        EntityUtils.consume(entity)
        IOUtils.closeQuietly(response)
      }
    }
    catch {
      case e: IOException => {
        throw new RuntimeException(e)
      }
    }
  }
}
