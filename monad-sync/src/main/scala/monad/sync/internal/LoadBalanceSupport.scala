// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.sync.internal

import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import monad.core.config.Partition
import monad.jni.services.gen._
import stark.utils.services.{LoggerSupport, StarkException}

/**
 * load balance support
 */
trait LoadBalanceSupport {
  this: SyncNoSQLSupport with LoggerSupport with SyncConfigLike =>
  protected val partitionInfoData = new ConcurrentHashMap[Short, PartitionInfo]()
  private val random = new Random()
  protected var servers: Array[PartitionInfo] = _

  /**
   * balance data with partition.
   * first look up the key,
   * if exists ,fetch partition information.
   * else balance partition
   * @param partitionMappingKey card partition mapping key
   * @param binlogOptions binlog options
   * @param command operation type
   */
  protected def balanceDataWithPartition(partitionMappingKey: PartitionMappingKey,
                                         binlogOptions: SyncBinlogOptions,
                                         command: DataCommandType): Option[PartitionInfo] = {

    binlogOptions.setCommand_type(command)

    val partitionMappingData = nosql.Get(partitionMappingKey)
    var partitionInfo: Option[PartitionInfo] = None

    if (partitionMappingData == null) {
      // partition mapping not found
      if (command == DataCommandType.DEL) {
        error("sync data not found for delete operation!")
        return partitionInfo
      }
      //balance server
      partitionInfo = Some(balanceServer())
      binlogOptions.setPartition_id(partitionInfo.get.partition.id)
    } else {
      // using old partition mapping value
      val partitionMappingValue = new PartitionMappingValue(partitionMappingData)
      binlogOptions.setPartition_id(partitionMappingValue.PartitionId())
      if (command == DataCommandType.PUT) {
        //说明已经分配好了数据，则操作变为更新
        binlogOptions.setCommand_type(DataCommandType.UPDATE)
      }
      partitionMappingValue.delete()

      partitionInfo = Some(partitionInfoData.get(binlogOptions.getPartition_id))
    }

    //set next binlog sequence
    binlogOptions.setSeq(partitionInfo.get.binlogSeq.incrementAndGet())
    partitionInfo
  }

  //负载均衡
  private[internal] def balanceServer(): PartitionInfo = {
    val length = servers.length // 总个数
    var totalWeight = 0 // 总权重
    var sameWeight = true // 权重是否都一样
    var i = 0
    var s: PartitionInfo = null
    while (i < length) {
      s = servers(i)
      i += 1
      val weight = s.partition.weight
      totalWeight += weight; // 累计总权重
      if (sameWeight) {
        sameWeight = totalWeight == (weight * i) //判断是否为相同的权重
      }
    }
    if (totalWeight > 0 && !sameWeight) {
      // 如果权重不相同且权重大于0则按总权重数随机
      var offset = random.nextInt(totalWeight)
      // 并确定随机值落在哪个片断上
      i = 0
      while (i < length) {
        s = servers(i)
        offset -= s.partition.weight
        if (offset < 0) {
          return s
        }
        i += 1
      }
    }
    // 如果权重相同或权重为0则均等随机
    servers(random.nextInt(length))
  }

  protected def loadPartitionInfo() = {
    val nodes = config.sync.nodes.iterator()
    while (nodes.hasNext) {
      val partition = nodes.next()
      if (partitionInfoData.contains(partition.id))
        throw new StarkException("duplicate partition(" + partition.id + ") definition in sync configuration.", MonadSyncExceptionCode.DUPLICATE_PARTITION_DEFINITION)
      val lastBinlogSeq = nosql.FindMaxBinlogSeqByPartitionId(partition.id)
      val dataSeq = findMaxDataSeqByPartitionId(partition.id)
      info("load partition:{},binlog:{},dataSeq:" + dataSeq, partition.id, lastBinlogSeq)
      partitionInfoData.put(partition.id, new PartitionInfo(partition, new AtomicLong(lastBinlogSeq), new AtomicInteger(dataSeq)))
    }
    servers = partitionInfoData.values().toArray(new Array[PartitionInfo](partitionInfoData.size()))
  }

  private def findMaxDataSeqByPartitionId(partitionId: Short): Int = {
    val dataSeqKey = new SyncPartitionDataSeqKey(partitionId)
    val result = nosql.Get(dataSeqKey)
    var dataSeq = 0
    if (result != null) {
      val dataSeqValue = new SyncPartitionDataSeqValue(result)
      dataSeq = dataSeqValue.Seq()
    }

    dataSeq
  }
}

case class PartitionInfo(partition: Partition, //分区
                         binlogSeq: AtomicLong, /*分区的binlog */
                         dataSeq: AtomicInteger)
