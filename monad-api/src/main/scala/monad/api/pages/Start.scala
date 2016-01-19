// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai. 
 * site: http://www.ganshane.com
 */

package monad.api.pages

import monad.face.model.{ResourceDefinition, ResourceType}
import monad.face.services.ResourceDefinitionLoader
import org.apache.tapestry5.annotations.{Cached, Property}
import org.apache.tapestry5.ioc.annotations.Inject
import org.apache.tapestry5.services.{Request, Response}
import org.apache.tapestry5.util.TextStreamResponse

import scala.collection.JavaConversions._

/**
 * init page
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.3
 */
class Start {
  private val XML: String =
    """|<?xml version="1.0"?>
      |<cross-domain-policy>
      |    <site-control permitted-cross-domain-policies="all" />
      |    <allow-access-from domain="*" />
      |    <allow-http-request-headers-from domain="*" headers="*"/>
      |</cross-domain-policy>
    """.stripMargin
  @Inject
  var loader: ResourceDefinitionLoader = _
  @Property
  var indexDef: ResourceDefinition = _
  @Inject
  var response:Response = _
  @Inject
  private var request: Request = _

  def onActivate(): Object = {
    response.setHeader("Access-Control-Allow-Origin","*");
    println(request.getPath);
    if (request.getPath.contains("crossdomain.xml")) {
      return new TextStreamResponse("text/xml", XML)
    }
    return null;
  }

  @Cached
  def getIndexNames = asJavaCollection(loader.getResourceDefinitions.toIterable.filterNot(_.resourceType == ResourceType.Data))
}
