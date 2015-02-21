package monad.sync.services

import monad.protocol.internal.InternalSyncProto.{SyncRequest, SyncResponse}
import monad.support.services.ServiceLifecycle

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-20
 */
trait ResourceImporterManager extends ServiceLifecycle {
  def fetchSyncData(request: SyncRequest): SyncResponse
}
