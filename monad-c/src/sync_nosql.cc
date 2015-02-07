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
uint32_t SyncNoSQL::FindOrLoadPartitionCount(uint8_t partition_id)
throw(monad::MonadStatus) {
  uint32_t count = _partition_counts[partition_id];
  if (count > 0 )
    return count;

  SyncPartitionDataCountKey key(partition_id);
  std::string value;
  MonadStatus status = RawGet(key.ToString(), &value);
  if (status.ok()) { //ok
    SyncPartitionDataCountValue data_count_value(value);
    count = data_count_value.Count();
    _partition_counts[partition_id] = count;
    return count;
  } else if (status.code() == kNotFound) {
    return 0;
  } else
    throw status;
}
MonadStatus SyncNoSQL::GetBinlogValue(const SyncBinlogKey &binlog_key,SyncBinlogValue& value) {
  std::string val;
  MonadStatus status = RawGet(binlog_key.ToString(), &val);
  if (status.ok()) {
    value.Assign(val);
  }
  return status;
}
MonadStatus SyncNoSQL::PutDataWithBinlog(const leveldb::Slice& key,
    const leveldb::Slice &data,
    const SyncBinlogOptions &binlog_options) {

  //得到分区的数据统计信息
  uint32_t count = FindOrLoadPartitionCount(binlog_options.partition_id);
  
  //create batch
  leveldb::WriteBatch batch;
  NormalSeqDataKey data_seq_key(binlog_options.partition_id, binlog_options.data_seq);
  
  //分区信息
  PartitionMappingKey partition_mapping_key(key.ToString());
  if (binlog_options.command_type == DEL) { //假如是删除模式
    batch.Delete(data_seq_key.ToString());
    batch.Delete(partition_mapping_key.ToString());
    count -= 1;
  } else {
    if (binlog_options.command_type == PUT) {
      //放入分区映射表
      PartitionMappingValue partition_mapping_value(binlog_options.partition_id,
                                                    binlog_options.data_seq);
      batch.Put(partition_mapping_key.ToString(), partition_mapping_value.ToString());
      //只有在PUT的时候数据才增加，更新下，数据不增加
      count += 1;
      //只有PUT的时候，才记录最大的数据序列ID
      SyncPartitionDataSeqKey partition_data_seq_key(binlog_options.partition_id);
      SyncPartitionDataSeqValue partition_data_seq_value(binlog_options.data_seq);
      batch.Put(partition_data_seq_key.ToString(), partition_data_seq_value.ToString());
    }
    //value的值由binlog_value保存，此处讲不在保存原始的值
    /*
     //在非删除情况下，如果有数据，则放入到数据库
     if(data.size() >0) { //有数据则放入到数据库
     batch.Put(data_seq_key.ToString(), data);
     }
     */
  }
  
  //写入同步使用的数据
  SyncBinlogKey binlog_key(binlog_options.partition_id, binlog_options.seq);
  SyncBinlogValue binlog_value(binlog_options.partition_id,
                               binlog_options.seq,
                               binlog_options.command_type,
                               data_seq_key,
                               data);
  batch.Put(binlog_key.ToString(), binlog_value.ToString());
  
  //写入分区的数据总量
  SyncPartitionDataCountKey data_count_key(binlog_options.partition_id);
  SyncPartitionDataCountValue data_count_value(count);
  batch.Put(data_count_key.ToString(), data_count_value.ToString());
  
  leveldb::WriteOptions write_opts;
  leveldb::Status l_status = _db->Write(write_opts, &batch);
  if (l_status.ok()) { //当且仅当写入成功之后才进行写入
    _partition_counts[binlog_options.partition_id] = count;
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
