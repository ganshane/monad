// Copyright 2011,2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/*
 * Copyright 2002-2010 Jun Tsai. 
 * site: http://www.ganshane.com
 */

package monad.api

import com.google.gson.JsonParser

/**
 * api constants
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.3
 */
object MonadApiConstants {
  final val JSON_MIME_TYPE = "text/plain"
  //服务器返回节点信息的Header
  final val HEADER_NODE_ALL = "X-Node-All"
  final val HEADER_NODE_SUCCESS = "X-Node-Success"
  final val HEADER_NODE_SUCCESS_INFO = "X-Node-Success-Info"
  final val HEADER_NODE_ERROR = "X-Node-Error"
  final val HEADER_ID_LENGTH = "X-Id-Length"
  final val HEADER_REGIONS = "X-Regions"
  final val HEADER_STARTS = "X-Starts"
  final val HEADER_ACCESS_CONTROL_ALLOW = "Access-Control-Allow-Origin"
  final val JSON_PARSER = new JsonParser()
  //API返回的JSON信息的Key
  final val JSON_KEY_DATA = "data"
  final val JSON_KEY_TOTAL = "total"
  final val JSON_KEY_ALL = "all"
  final val JSON_KEY_SUCCESS = "success"
  final val JSON_KEY_NODE_ALL = "node.all"
  final val JSON_KEY_NODE_SUCCESS = "node.success"
  final val JSON_KEY_NODE_SUCCESS_INFO = "node.success.info"
  final val JSON_KEY_NODE_ERROR = "node.error"
  final val JSON_KEY_TOTAL_RECORD_NUM = "total_record_num"
  //error status
  private[monad] final val ERROR_STATUS = 1
  //ok status
  private[monad] final val OK_STATUS = 0
  //status key
  private[monad] final val STATUS = "status"
  //message key
  private[monad] final val MSG = "msg"
}
