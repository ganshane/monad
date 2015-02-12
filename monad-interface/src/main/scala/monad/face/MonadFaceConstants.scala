// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face

import java.nio.charset.Charset

import com.google.gson.{JsonParser, GsonBuilder}
import org.apache.lucene.util.Version

/**
 *
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-12
 */
object MonadFaceConstants {
  final val DEFAULT_RESOURCE_SEARCHER_FACTORY="default"
  final val NODE_GROUP_NAME="monad-index-group"
  final val NODE_GROUP_PASS="monad-index-pass"
  final val LUCENE_VERSION=Version.LATEST

  final val RESYNC_RESOURCE="rsync"
  final val GLOBAL_GSON = new GsonBuilder().disableHtmlEscaping().create()
  /*
  final lazy val SYNC_DISRUPTOR_EXECUTOR = Executors.newFixedThreadPool(2,new ThreadFactory {
      private val seq = new AtomicInteger()
      def newThread(p1: Runnable) = {
          val thread = new Thread(p1)
          thread.setName("monad-disruptor-%d".format(seq.incrementAndGet()))
          //设置优先级，让抽取和索引线程滞后处理
          thread.setPriority((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/2)
          thread
      }
  })
  */
  final val LIFE_CLOUD="cloud"
  final val LIFE_GROUP_NOTIFIER="groupNotifier"
  final val LIFE_GROUP_ZOOKEEPER="groupZookeeper"
  final val LIFE_RPC="rpc"
  final val LIFE_RPC_CLIENT="rpcClient"
  final val LIFE_NOSQL="nosql"
  final val LIFE_INDEXER="indexer"
  final val LIFE_IMPORTER="importer"
  final val LIFE_RESOURCES="resources"
  final val LIFE_ID_SERVER="id"

  final val NUM_OF_NEED_COMMIT= (1 << 16) -1
  final val NUM_OF_NEED_UPDATE_REGION_INFO = (1 << 19) -1

  //nosql 类型
  final val BDB_NOSQL="Bdb"
  final val LEVEL_DB_NOSQL="Leveldb"


  final val UTF8_ENCODING_CHARSET=Charset.forName("UTF-8")
  final val GLOBAL_GSON_PARSER = new JsonParser

  /*Index Constant*/
  final val OBJECT_ID_FIELD_NAME="_id"
  final val UPDATE_TIME_FIELD_NAME="_ut"
  final val OBJECT_ID_PAYLOAD_FIELD="_PL"
  final val OBJECT_ID_PAYLOAD_VALUE="_UID"
  //进行频次搜索时候，输出结果中的次数
  final val FACET_COUNT = "_count"
  final val DYNAMIC_DESC = "_desc"

  /** 结果中的字符串高亮显示 **/
  final val HIGHLIGHT_PREFIX="<em>"
  final val HIGHLIGHT_SUFFIX="</em>"

}
