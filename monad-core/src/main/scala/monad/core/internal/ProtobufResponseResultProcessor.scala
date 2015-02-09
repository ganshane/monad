// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import java.io.OutputStream

import com.google.protobuf.GeneratedMessage
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.apache.tapestry5.services.{ComponentEventResultProcessor, Response}

/**
 * Created by jcai on 14-8-24.
 */
class ProtobufResponseResultProcessor(response: Response) extends ComponentEventResultProcessor[GeneratedMessage] {
  override def processResultValue(value: GeneratedMessage): Unit = {
    var os: OutputStream = null

    // The whole point is that the response is in the hands of the StreamResponse;
    // if they want to compress the result, they can add their own GZIPOutputStream to
    // their pipeline.

    response.disableCompression()

    //streamResponse.prepareResponse(response)
    try {
      os = response.getOutputStream("application/binary")
      value.writeTo(os)
      os.close()
      os = null
    }
    finally {
      InternalUtils.close(os)
    }

  }
}
