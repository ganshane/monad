package monad.sync.services

import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-20
 */
trait ResourceImporterManager {
  def fetchSyncData(request: SyncRequest): SyncResponse
}
