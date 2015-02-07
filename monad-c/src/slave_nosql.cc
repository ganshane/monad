// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#include "monad_config.h"

#ifdef MONAD_HAVE_ROCKSDB
#include "rocksdb/write_batch.h"
#else
#include "leveldb/write_batch.h"
#endif

#include "nosql_support.h"


namespace monad {
  MonadStatus SlaveNoSQLSupport::PutSlaveBinlog(const SlaveBinlogValue &binlog_value, const leveldb::Slice &data) {
    uint32_t tmp_count = _data_count;
    leveldb::WriteBatch batch;
    //写入原始数据
    std::string data_key = binlog_value.Key();
    if (binlog_value.CommandType() == DEL) { //根据同步的命令来进行对应的操作
      batch.Delete(data_key);
      tmp_count -= 1;
    } else {
      if (data.size() > 0)
        batch.Put(data_key, data);
    }
    //当且仅当是PUT的时候，才进行数据更新
    if (binlog_value.CommandType() == PUT)
      tmp_count += 1;

    //写入同步日志
    SlaveBinlogKey key(binlog_value.Seq());
    batch.Put(key.ToString(), binlog_value.ToString());
    LogDebug("write seq:%lld binlog seql:%lld ", key.Seq(), binlog_value.Seq());

    //写入数据统计数据
    SlaveDataCountKey count_key;
    SlaveDataCountValue count_value(tmp_count);
    batch.Put(count_key.ToString(), count_value.ToString());

    leveldb::Status status = _db->Write(leveldb::WriteOptions(), &batch);
    if (status.ok()) {
      _data_count = tmp_count;
    }

    return MonadStatus::FromLeveldbStatus(status);
  }
  MonadStatus SlaveNoSQLSupport::PutBinlog(const SyncBinlogValue &binlog_value) {
    std::string slave_binlog = binlog_value.ToSlaveBinlogString();
    std::string value = binlog_value.Value();
    SlaveBinlogValue slave_binlog_value(slave_binlog);
    return PutSlaveBinlog(slave_binlog_value, value);
  }
  uint64_t SlaveNoSQLSupport::FindLastBinlog() {

    uint64_t ret = 0;
    //构造最大binlog的key
    SlaveBinlogKey min_key(0);
    SlaveBinlogKey max_key(UINT64_MAX);

    std::string *value = FindMaxKeyInRange(min_key.ToString(), max_key.ToString());
    if (value != NULL) {
      SlaveBinlogKey log_key(*value);
      ret = log_key.Seq();
      delete value;
    }
    return ret;
  }
  SyncBinlogValue *SlaveNoSQLSupport::FindNextBinlog(uint64_t binlog_seq) {
    SlaveBinlogKey log_key(binlog_seq);
    SlaveBinlogKey max_key(UINT64_MAX);

    SyncBinlogValue *val = NULL;
    leveldb::ReadOptions iterate_options;
    leveldb::Iterator *it = _db->NewIterator(iterate_options);
    it->Seek(log_key.ToString());

    if (it->Valid()) {
      std::string key_str = it->key().ToString();
      if (key_str < max_key.ToString() && key_str >= log_key.ToString()) {
        leveldb::Slice value = it->value();
        val = new SyncBinlogValue(value);
      }
    }
    delete it;

    return val;
  }

  MonadStatus SlaveNoSQLSupport::DeleteBinlogRange(const uint64_t from, const uint64_t to) {
    uint64_t start = from;
    while (start <= to) {
      leveldb::WriteBatch batch;
      for (int count = 0; start <= to && count < 1000; start++, count++) {
        batch.Delete(SlaveBinlogKey(start).ToString());
      }
      leveldb::Status s = _db->Write(leveldb::WriteOptions(), &batch);
      if (!s.ok()) {
        return MonadStatus::FromLeveldbStatus(s);
      }
    }
    return MonadStatus::OK();
  }
}
