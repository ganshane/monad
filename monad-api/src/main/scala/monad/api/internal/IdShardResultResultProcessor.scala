// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.internal
//package org.apache.lucene.util

import java.io.OutputStream

import monad.api.MonadApiConstants
import monad.face.model.IdShardResult
import monad.face.services.BitSetUtils
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.{ComponentEventResultProcessor, Response}

/**
 * 针对ID搜索返回的结果进行处理
 * @author jcai
 */
class IdShardResultResultProcessor(response: Response) extends ComponentEventResultProcessor[IdShardResult] {
  def processResultValue(collect: IdShardResult) {
    var os: OutputStream = null;

    // The whole point is that the response is in the hands of the StreamResponse;
    // if they want to compress the result, they can add their own GZIPOutputStream to
    // their pipeline.

    //response.disableCompression();


    try {

      response.setHeader(MonadApiConstants.HEADER_ACCESS_CONTROL_ALLOW, "*")
      response.setHeader(MonadApiConstants.HEADER_NODE_ALL, "16") //collect.nodesAll.toString)
      response.setHeader(MonadApiConstants.HEADER_NODE_SUCCESS,"11")// "collect.nodesSuccess.toString)

      //if (collect.nodesSuccessInfo != null)
      //  response.setHeader(MonadApiConstants.HEADER_NODE_SUCCESS_INFO, collect.nodesSuccessInfo.toString)
      //response.setHeader(MonadApiConstants.HEADER_NODE_ERROR, collect.nodesError.toString)

      val regionLength = if(collect.data != null) 1 else 0
      response.setHeader(MonadApiConstants.HEADER_REGIONS,0.until(regionLength).mkString(","))

      //os = response.getOutputStream("text/plain")
      //os.write(("size:"+collect.data.cardinality()).getBytes)

      //os = new DeflaterOutputStream(response.getOutputStream("application/octet-stream"),true)
      os = response.getOutputStream("application/octet-stream")
      BitSetUtils.serialize(collect.data,os)

      /*
      val byteBuffer = ByteBuffer.allocate(8)
      for ((shardResult, index) <- collect.results.view.zipWithIndex) {
        val value = shardResult.data
        val v1= value.length()
        val v2= value.bits
        val v3= value.indices
        val v4= value.nonZeroLongCount
        val v5= value.ramBytesUsed()
        /*
        //TODO 编译错误
        val len = 0 //value.getNumWords
        val bits = value.getBits
        for (i <- 0 until len) {
          //System.out.println("write long "+bits(i))
          byteBuffer.position(0)
          byteBuffer.putLong(bits(i))
          os.write(byteBuffer.array())
          //writeLong(os,bits(i))
        }
        */
      }
      */

      os.flush()
      os.close()
      os = null

    }
    finally {
      InternalUtils.close(os)
    }
  }

  private def writeLong(os: OutputStream, l: Long) {
    for (i <- 0 until 8) {
      os.write((l >>> (i << 3)).asInstanceOf[Int])
    }
  }
}
