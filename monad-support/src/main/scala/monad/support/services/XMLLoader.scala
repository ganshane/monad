// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.io._
import javax.xml.XMLConstants
import javax.xml.bind.util.ValidationEventCollector
import javax.xml.bind.{JAXBContext, Marshaller}
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl
import monad.support.MonadSupportConstants
import org.w3c.dom.ls.{LSInput, LSResourceResolver}

import scala.io.Source
import scala.util.control.NonFatal

/**
 * 使用Jaxb方式解析XML配置文件
 *
 * @author jcai
 * @version 0.1
 */
object XmlLoader {
  def loadConfig[T <: Object](clazz: Class[T], filePath: String): T = {
    loadConfig[T](filePath)(Manifest.classType(clazz))
  }

  /** 加载某一个配置 **/
  def loadConfig[T <: Object](filePath: String,
                              symbols: Map[String, String] = Map[String, String](),
                              encoding: String = MonadSupportConstants.UTF8_ENCODING)(implicit m: Manifest[T]): T = {
    val content = Source.fromFile(filePath, encoding).mkString
    parseXML(content, symbols, encoding)
  }

  def parseXML[T <: Object](content: String,
                            symbols: Map[String, String] = Map[String, String](),
                            encoding: String = MonadSupportConstants.UTF8_ENCODING,
                            xsd: Option[InputStream] = None)(implicit m: Manifest[T]): T = {
    parseXML(new ByteArrayInputStream(SymbolExpander.expand(content, symbols).getBytes(encoding)), xsd)
  }

  def parseXML[T <: Object](is: InputStream, xsd: Option[InputStream])(implicit m: Manifest[T]): T = {
    val vec = new ValidationEventCollector()
    try {
      //obtain type parameter
      val clazz = m.runtimeClass.asInstanceOf[Class[T]]
      //create io reader
      val reader = new InputStreamReader(is, MonadSupportConstants.UTF8_ENCODING)
      val context = JAXBContext.newInstance(clazz)
      //unmarshal xml
      val unmarshaller = context.createUnmarshaller()
      //.unmarshal(reader).asInstanceOf[T]
      if (xsd.isDefined) {
        val sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        sf.setResourceResolver(new LSResourceResolver {
          override def resolveResource(`type`: String, namespaceURI: String, publicId: String, systemId: String, baseURI: String): LSInput = {
            val input = new DOMInputImpl()
            if (systemId.endsWith("monad.xsd")) {
              //仅仅处理系统的文件 TODO 调整为能够自动识别文件路径
              input.setByteStream(getClass.getResourceAsStream("/monad.xsd"))
            }
            input
          }
        })
        val schemaSource = new StreamSource(xsd.get, "xml")
        val schema = sf.newSchema(schemaSource)
        unmarshaller.setSchema(schema)
        unmarshaller.setEventHandler(vec)
      }
      unmarshaller.unmarshal(reader).asInstanceOf[T]
    } catch {
      case NonFatal(e) =>
        throw MonadException.wrap(e, MonadSupportErrorCode.FAIL_PARSE_XML)
    } finally {
      close(is)
      if (xsd.isDefined)
        close(xsd.get)
      if (vec.hasEvents) {
        val veOption = vec.getEvents.headOption
        if (veOption.isDefined) {
          val ve = veOption.get
          val vel = ve.getLocator
          throw new MonadException(
            "line %s column %s :%s".format(vel.getLineNumber, vel.getColumnNumber, ve.getMessage),
            MonadSupportErrorCode.FAIL_PARSE_XML)
        }
      }
    }
  }

  private def close(io: Closeable) {
    try {
      io.close()
    } catch {
      case NonFatal(e) =>
    }
  }

  /**
   * 把对象转化为XML文件
   */
  def toXml[T](obj: T, encoding: String = MonadSupportConstants.UTF8_ENCODING): String = {
    val context = JAXBContext.newInstance(obj.getClass)
    val marshaller = context.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    val out = new ByteArrayOutputStream
    marshaller.marshal(obj, out)
    new String(out.toByteArray, encoding)
  }
}
