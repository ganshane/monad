package monad.api.internal

import java.io.OutputStream
import java.util.zip.DeflaterOutputStream

import monad.api.MonadApiConstants
import monad.face.model.OpenBitSetWithNodes
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.{ComponentEventResultProcessor, Response}

/**
 *
 * @author jcai
 */
class OpenBitSetResultProcessor(response: Response) extends ComponentEventResultProcessor[OpenBitSetWithNodes] {
  def processResultValue(openBitSetWithNodes: OpenBitSetWithNodes) {
    var os: OutputStream = null;

    // The whole point is that the response is in the hands of the StreamResponse;
    // if they want to compress the result, they can add their own GZIPOutputStream to
    // their pipeline.

    response.disableCompression();


    try {

      response.setHeader(MonadApiConstants.HEADER_NODE_ALL, openBitSetWithNodes.nodesAll.toString)
      response.setHeader(MonadApiConstants.HEADER_NODE_SUCCESS, openBitSetWithNodes.nodesSuccess.toString)
      if (openBitSetWithNodes.nodesSuccessInfo != null)
        response.setHeader(MonadApiConstants.HEADER_NODE_SUCCESS_INFO, openBitSetWithNodes.nodesSuccessInfo.toString)
      response.setHeader(MonadApiConstants.HEADER_NODE_ERROR, openBitSetWithNodes.nodesError.toString)
      response.setHeader(MonadApiConstants.HEADER_ID_LENGTH, openBitSetWithNodes.bitSet.cardinality().toString)

      os = new DeflaterOutputStream(response.getOutputStream("application/octet-stream"))
      val value = openBitSetWithNodes.bitSet
      //删除尾部的0，缩短长度
      value.trimTrailingZeros()
      val len = value.getNumWords
      val bits = value.getBits

      for (i <- 0 until len) {
        //os.write(DataTypeUtils.convertAsArray(bits(i)))
        writeLong(os, bits(i))
      }
      /*
      val it = value.iterator()
      while(it.nextDoc() != DocIdSetIterator.NO_MORE_DOCS){
          os.write(DataTypeUtils.convertIntAsArray(it.docID()))
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
