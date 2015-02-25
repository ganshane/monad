package monad.api.pages.api.analytics

import javax.inject.Inject

import monad.face.services.RpcSearcherFacade
import org.apache.tapestry5.services.Request

/**
 * 导入身份证号码
 * @author jcai
 */
class ImportIdCardsApi {
  @Inject
  private var request: Request = _
  @Inject
  private var idFacade: RpcSearcherFacade = _

  def onActivate() = {
    /*
    val ids = request.getParameter("q").split(",")
    val bitSet = new OpenBitSet(10240)
    //TODO 并行
    val map = mutable.Map[Short, OpenBitSet]()
    ids.foreach { id =>
      if (!id.isEmpty) {
        val i = idFacade.findObjectIdSeq(id)
        if (i.isDefined) {
          val shard = i.get
          map.get(shard.region) match {
            case Some(regionBitSet) =>
              regionBitSet.set(shard.seq)
            case None =>
              val regionBitSet = new OpenBitSet(10240)
              regionBitSet.set(shard.seq)
              map.put(shard.region, regionBitSet)
          }
        }
      }
    }

    val allShard = map.map {
      case (region, regionBitSet) =>
        val result = new IdShardResult
        result.data = regionBitSet
        result.region = region
        result
    }.toArray
    val collect = new IdShardResultCollect()
    collect.results = allShard

    return collect
      */
  }
}
