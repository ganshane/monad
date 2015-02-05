// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#include "monad_config.h"
#include "sync_nosql.h"

#ifdef MONAD_HAVE_ROCKSDB
#include "rocksdb/write_batch.h"
#else
#include "leveldb/write_batch.h"
#endif

namespace monad {
uint32_t SyncNoSQL::FindOrLoadPartitionCount(uint8_t partition_id, uint32_t data_type)
throw(monad::MonadStatus) {
  std::pair<uint8_t, int32_t> count_key = std::make_pair(partition_id, data_type);
  uint32_t count = _partition_counts[count_key];
  if (count > 0 )
    return count;

  SyncPartitionDataCountKey key(partition_id, data_type);
  std::string value;
  MonadStatus status = RawGet(key.ToString(), &value);
  if (status.ok()) { //ok
    SyncPartitionDataCountValue data_count_value(value);
    count = data_count_value.Count();
    _partition_counts[count_key] = count;
    return count;
  } else if (status.code() == kNotFound) {
    return 0;
  } else
    throw status;
}
std::string *SyncNoSQL::GetBinlogValue(const SyncBinlogKey &binglog_key) {
  std::string *val = new std::string();
  MonadStatus status = RawGet(binglog_key.ToString(), val);
  if (!status.ok() || status.code() == kNotFound) {
    delete val;
    return NULL;
  }
  SlaveBinlogValue binlog_value(*val);
  if (binlog_value.CommandType() == PUT || binlog_value.CommandType() == UPDATE) { //假如是put，则进行处理抓取原始数据
    std::string origin_data_key = binlog_value.Key();
    std::string origin_data_value;
    status = RawGet(origin_data_key, &origin_data_value);
    if (status.ok() || status.code() == kNotFound) {
      val->append(origin_data_value);
    } else {
      delete val;
      return NULL;
    }
  }
  return val;
}
MonadStatus SyncNoSQL::PutDataWithBinlog(const BaseBufferSupport &key,
    const int64_t card_id,
    const leveldb::Slice &data,
    const SyncBinlogOptions &binlog_options) {

  //得到分区的数据统计信息
  uint32_t count = FindOrLoadPartitionCount(binlog_options.partition_id, binlog_options.data_type);
  uint32_t old_count  = count;

  //create batch
  leveldb::WriteBatch batch;


  
  DataCommandType command = binlog_options.command_type;
  
  //分区信息
  CardPartitionMappingKey partition_mapping_key(card_id, binlog_options.data_type);
  if (command == DEL) { //假如是删除模式
    //batch.Delete(partition_mapping_key.ToString());
    batch.Delete(key.ToString());
    count -= 1;
  } else {
    if (command == PUT) {
      //放入分区映射表
      PartitionMappingValue partition_mapping_value(binlog_options.partition_id);
      batch.Put(partition_mapping_key.ToString(), partition_mapping_value.ToString());
      count += 1;
    }
    //针对原始数据的操作命令
    if(ExistData(key)){
      command = UPDATE;
    }else{
      command = PUT;
    }
    //在非删除情况下，如果有数据，则放入到数据库
    if (data.size() > 0)
      batch.Put(key.ToString(), data);
  }

  //写入binlog日志
  SyncBinlogKey binlog_key(binlog_options.partition_id, binlog_options.seq);
  SlaveBinlogValue binlog_value(binlog_options.partition_id,
                                binlog_options.seq,
                                command,
                                key);
  batch.Put(binlog_key.ToString(), binlog_value.ToString());
  LogDebug("Partition_id:%d seq2:%lld ", binlog_options.partition_id, binlog_key.Seq());
  //写入时间戳
  DataTimestampKey data_timestamp_key(binlog_options.data_type);
  DataTimestampValue data_timestamp_value(binlog_options.timestamp);
  batch.Put(data_timestamp_key.ToString(), data_timestamp_value.ToString());

  if (old_count != count) {
    //写入分区的数据总量
    SyncPartitionDataCountKey data_count_key(binlog_options.partition_id,
        binlog_options.data_type);
    SyncPartitionDataCountValue data_count_value(count);
    batch.Put(data_count_key.ToString(), data_count_value.ToString());
  }

  leveldb::WriteOptions write_opts;
  leveldb::Status l_status = _db->Write(write_opts, &batch);
  if (l_status.ok()) { //写入成功之后，更新变量值
    std::pair<uint8_t, int32_t> count_key = std::make_pair(binlog_options.partition_id, binlog_options.data_type);
    _partition_counts[count_key] = count;
  }
  return MonadStatus::FromLeveldbStatus(l_status);
}
uint64_t SyncNoSQL::FindMaxBinlogSeqByPartitionId(const uint8_t partition_id) {
  SyncBinlogKey min_key(partition_id, 0);
  SyncBinlogKey max_key(partition_id, UINT64_MAX);
  std::string *value = this->FindMaxKeyInRange(min_key.ToString(), max_key.ToString());
  uint64_t ret = 0;
  if (value) {
    SyncBinlogKey key(*value);
    ret = key.Seq();
    delete value;
  }
  return ret;
}
uint64_t SyncNoSQL::FindMinBinlogSeqByPartitionId(const uint8_t partition_id) {
  SyncBinlogKey min_key(partition_id, 0);
  SyncBinlogKey max_key(partition_id, UINT64_MAX);
  std::string *value = this->FindMinKeyInRange(min_key.ToString(), max_key.ToString());
  uint64_t ret = 0;
  if (value) {
    SyncBinlogKey key(*value);
    ret = key.Seq();
    delete value;
  }
  return ret;
}
MonadStatus SyncNoSQL::DeleteBinlogRange(const uint8_t partition_id, const uint64_t from, const uint64_t to) {
  uint64_t start = from;
  while (start <= to) {
    leveldb::WriteBatch batch;
    for (int count = 0; start <= to && count < 1000; start++, count++) {
      batch.Delete(SyncBinlogKey(partition_id, start).ToString());
    }
    leveldb::Status s = _db->Write(leveldb::WriteOptions(), &batch);
    if (!s.ok()) {
      return MonadStatus::FromLeveldbStatus(s);
    }
  }
  return MonadStatus::OK();
}
}//nirvana
