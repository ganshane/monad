// Copyright 2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.services

import java.util.concurrent.Future

import monad.face.services.ResourceDefinitionLoaderListener
import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.sync.internal.ResourceImporter


/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-20
 */
trait ResourceImporterManager extends ResourceDefinitionLoaderListener {
  def fetchSyncData(request: SyncRequest): SyncResponse

  def importData(resourceName: String,
                 row: Array[Any],
                 timestamp: Long,
                 version: Int)

  def resync(resourceName: String)

  def submitSync(fun: => Unit): Future[_]

  def getObject(resourceName: String): ResourceImporter
}
