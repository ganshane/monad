// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_STORAGE_SYNC_NOSQL_H_
#define MONAD_STORAGE_SYNC_NOSQL_H_

#include <map>
#include <utility>
#include "status.h"
#include "nosql_support.h"

namespace monad {
/**
 * 同步binlog的参数
 */
struct SyncBinlogOptions {
  /* 分区的ID */
  uint8_t partition_id;
  /* 数据操作类型 */
  DataCommandType command_type;
  /* binlog的序号 */
  uint64_t seq;
  /* 此data_type作为关键字记录时间戳 */
  uint32_t data_type;
  /* 数据的时间戳 */
  uint64_t timestamp;
 public:
  SyncBinlogOptions() {
    partition_id = 0;
    command_type = COMMAND_UNKNOWN;
    seq = 0;
    data_type = 0;
    timestamp = 0;
  }
};
/**
 * 同步服务器的NoSQL实现，用于存储binlog以及原始数据,
 * 其他节点服务器从此NoSQL中取得数据
 */
class SyncNoSQL: public NoSQLSupport {
 private:
  std::map<std::pair<uint8_t, int32_t>, uint32_t> _partition_counts;
 public:
  /**
   * 查找某一分区某一数据的个数
   */
  uint32_t FindOrLoadPartitionCount(uint8_t partition_id, uint32_t data_type)
  throw(monad::MonadStatus);
  /**
   * 构造SyncNoSQL实例
   * @param db_path 数据库路径
   * @param options 数据库的参数
   */
  SyncNoSQL(const char *db_path, const NoSQLOptions &options): NoSQLSupport(db_path, options) {};
  /**
   * 同步数据
   * @param key Key值
   * @param data 数据
   * @param binlog_options 数据同步的参数
   * @param 给出返回的状态
   */
  virtual MonadStatus PutDataWithBinlog(const BaseBufferSupport &key,
      const int64_t card_id,
      const leveldb::Slice &data,
      const SyncBinlogOptions &binlog_options);
  /**
   * 得到binlog的值
   */
  virtual std::string *GetBinlogValue(const SyncBinlogKey &binglog_key);

  /**
   * 通过给定的分区ID，来取得binlog的最大值
   * @param partition_id 分区ID
   * @return 返回改对应分区的最大值
   */
  uint64_t FindMaxBinlogSeqByPartitionId(const uint8_t partition_id);
  /**
   * 通过给定的分区ID，来取得binlog的最小值
   * @param partition_id 分区ID
   * @return 返回改对应分区的最大值
   */
  uint64_t FindMinBinlogSeqByPartitionId(const uint8_t partition_id);
  /**
   * 删除从from位置的binlog日志，到结束点的日志
   * @param region_id 分区ID
   * @param from 开始位置
   * @param to 结束位置
   * @return 状态
   */
  MonadStatus DeleteBinlogRange(const uint8_t partition_id,
                                        const uint64_t from,
                                        const uint64_t to);
};
}//namespace monad
#endif //MONAD_STORAGE_SYNC_NOSQL_H_
