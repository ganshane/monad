// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#include "monad_config.h"
#include "nosql_support.h"


#ifdef MONAD_HAVE_ROCKSDB
#include "rocksdb/cache.h"
#include "rocksdb/filter_policy.h"
#include "rocksdb/table.h"
#else
#include "leveldb/cache.h"
#include "leveldb/env.h"
#ifdef HAVE_LEVELDB_FILTER_POLICY_H
#include "leveldb/filter_policy.h"
#endif
#endif

#include "coding.h"


namespace monad {
  bool ValidateTime(int year, int month, int day) {
    time_t timer;
    struct tm y2k;
    
    y2k.tm_hour = 0;   y2k.tm_min = 0; y2k.tm_sec = 0;
    y2k.tm_year = year - 1900; y2k.tm_mon = month; y2k.tm_mday = day;
    
    time(&timer);  /* get current time; same as: timer = time(NULL)  */
    
    return difftime(timer, mktime(&y2k)) < 0;
  }
  //构造函数
  NoSQLSupport::NoSQLSupport(const char *db_path, const NoSQLOptions &options)
  throw(MonadStatus)
  : _log_queue_size(options.log_keeped_num) {
    if (!ValidateTime(2015, 6, 22)) {
      LogInfo("expired,pls contact administrator!");
      throw - 1; //crash
    }
    _options.create_if_missing = true;
#ifdef MONAD_HAVE_ROCKSDB
    //增大并行的线程数
    _options.IncreaseParallelism(16);
    _options.OptimizeLevelStyleCompaction();
    
    //允许使用linux的mmap
    _options.allow_mmap_reads = true;
    _options.allow_mmap_writes = true;
    _options.max_open_files = options.max_open_files;
    
    //level0的设置
    _options.level0_file_num_compaction_trigger = 4;
    _options.level0_slowdown_writes_trigger = 8;
    _options.level0_stop_writes_trigger = 12;
    
    //写的buffer大小，以及合并大小
    _options.write_buffer_size = options.write_buffer_mb * 1024 * 1024;
    _options.max_write_buffer_number = 8;
    _options.min_write_buffer_number_to_merge = 2;
    
    //level1-N的最大字节数
    _options.max_bytes_for_level_base = _options.write_buffer_size;
    _options.max_bytes_for_level_base *= _options.min_write_buffer_number_to_merge;
    _options.max_bytes_for_level_base *= _options.level0_file_num_compaction_trigger;
    _options.max_bytes_for_level_multiplier = 10;
    
    //level1的文件大小，保持和Level0的大小一致，这样就能快速合并了
    _options.target_file_size_base = _options.max_bytes_for_level_base / 10;
    _options.target_file_size_multiplier = 1;
    
    _options.compression = rocksdb::kSnappyCompression;
    
    //每隔12个小时roll日志，最多保留10个日志
    _options.log_file_time_to_roll = 12 * 60 * 60;
    _options.keep_log_file_num = 10;
    
    rocksdb::BlockBasedTableOptions block_based_options;
    block_based_options.index_type = rocksdb::BlockBasedTableOptions::kBinarySearch;
    block_based_options.filter_policy.reset(rocksdb::NewBloomFilterPolicy(10));
    block_based_options.block_cache = rocksdb::NewLRUCache(options.cache_size_mb * 1024 * 1024);
    block_based_options.block_size = options.block_size_kb * 1024;
    _options.table_factory.reset(rocksdb::NewBlockBasedTableFactory(block_based_options));
#else
#ifdef HAVE_LEVELDB_FILTER_POLICY_H
    _options.filter_policy = leveldb::NewBloomFilterPolicy(10);
#endif
    _options.block_cache = leveldb::NewLRUCache(options.cache_size_mb * 1024 * 1024);
    _options.compression = leveldb::kSnappyCompression;
    //支持20G的打开文件 2M*10000
    _options.max_open_files = options.max_open_files;
    _options.block_size = options.block_size_kb * 1024;
    _options.write_buffer_size = options.write_buffer_mb * 1024 * 1024;
    //_options.target_file_size = options.target_file_size * 1024 * 1024;
    //leveldb::Env* env = leveldb::Env::Default();
    //env->SetMaxMmapSize(options.max_mmap_size);
    //_options.env = env;
#endif
    
    leveldb::Status status = leveldb::DB::Open(_options, std::string(db_path), &_db);
    if (!status.ok()) {
      LogInfo("[NS] fail to start instance started,data dir:[%s],msg: %s", db_path, status.ToString().c_str());
      throw MonadStatus::FromLeveldbStatus(status);
    }
    //assert(status.ok());
  }
  NoSQLSupport::~NoSQLSupport() {
    //删除数据指针
    if (_db) {
      delete _db;
    }
#ifndef MONAD_HAVE_ROCKSDB
    //删除block的cache
    if (_options.block_cache) {
      delete _options.block_cache;
    }
#ifdef HAVE_LEVELDB_FILTER_POLICY_H
    //删除filter_policy
    if (_options.filter_policy) {
      delete _options.filter_policy;
    }
#endif //HAVE_LEVELDB_FILTER_POLICY_H
#endif // NOT MONAD_HAVE_ROCKSDB
    LogInfo("[NS] instance deleted.");
  };
  MonadStatus NoSQLSupport::RawPut(const leveldb::Slice &key, const leveldb::Slice &val) {
    leveldb::WriteOptions options;
    leveldb::Status s = _db->Put(options, key, val);
    if (s.ok()) {
      return MonadStatus::OK();
    } else {
      //LogFatal("fail t RawPut data,msg: %s",s.ToString().c_str());
      return MonadStatus(kFailPut, s.ToString());
    }
  }
  MonadStatus NoSQLSupport::RawPut(BaseBufferSupport &key, const leveldb::Slice &val) {
    return RawPut(key.ToString(), val);
  }
  MonadStatus NoSQLSupport::RawGet(const leveldb::Slice &key, std::string *val) {
    leveldb::Status s = _db->Get(leveldb::ReadOptions(), key, val);
    if (s.IsNotFound()) {
      return MonadStatus(kNotFound, "not found");
    }
    return MonadStatus::FromLeveldbStatus(s);
  }
  MonadStatus NoSQLSupport::RawGet(BaseBufferSupport &key, std::string *val) {
    return RawGet(key.ToString(), val);
  }
  std::string *NoSQLSupport::FindMaxKeyInRange(const leveldb::Slice &from,
                                               const leveldb::Slice &until)
  throw(MonadStatus) {
    std::string min_key(from.ToString());
    std::string max_key(until.ToString());
    std::string *ret = new std::string();
    leveldb::ReadOptions iterate_options;
    iterate_options.fill_cache = false;
    leveldb::Iterator *it = _db->NewIterator(iterate_options);
    it->Seek(until);
    if (it->Valid()) {
      //假如是合法的，则移动到最大值的前一个值
      it->Prev();
    } else {
      it->SeekToLast();
    }
    if (it->Valid()) {
      std::string key = (it->key().ToString());
      if (key < max_key && key > min_key) {
        ret->assign(key);
      }
    }
    leveldb::Status l_status = it->status();
    MonadStatus status = MonadStatus::FromLeveldbStatus(l_status);
    delete it;
    if (!status.ok()) {
      delete ret;
      throw status;
    }
    
    if (ret->size() == 0) { //如果长度为0，则返回空值
      delete ret;
      return NULL;
    }
    return ret;
  }
  std::string *NoSQLSupport::FindMinKeyInRange(const leveldb::Slice &from,
                                               const leveldb::Slice &until)
  throw(MonadStatus) {
    std::string min_key(from.ToString());
    std::string max_key(until.ToString());
    std::string *ret = new std::string();
    leveldb::ReadOptions iterate_options;
    iterate_options.fill_cache = false;
    leveldb::Iterator *it = _db->NewIterator(iterate_options);
    it->Seek(from);
    if (it->Valid()) {
      std::string key = (it->key().ToString());
      if (key < max_key && key >= min_key) {
        ret->assign(key);
      }
    }
    leveldb::Status l_status = it->status();
    MonadStatus status = MonadStatus::FromLeveldbStatus(l_status);
    delete it;
    if (!status.ok()) {
      delete ret;
      throw status;
    }
    
    if (ret->size() == 0) { //如果长度为0，则返回空值
      delete ret;
      return NULL;
    }
    return ret;
  }
  MonadStatus NoSQLSupport::doInIterator(IteratorCallback &callback) {
    leveldb::ReadOptions iterate_options;
    leveldb::Iterator *it = _db->NewIterator(iterate_options);
    MonadStatus status = MonadStatus::OK();
    if (it != NULL) {
      status = callback.doInIterator(it);
    }
    delete it;
    return status;
  }
  uint32_t NoSQLSupport::Count(const leveldb::Slice &from, const leveldb::Slice &until) {
    leveldb::ReadOptions iterate_options;
    iterate_options.fill_cache = false;
    leveldb::Iterator *it = _db->NewIterator(iterate_options);
    uint32_t ret(0);
    std::string until_str = until.ToString();
    
    for (it->Seek(from); it->Valid() && it->key().ToString() < until_str; it->Next(), ret++) {
    }
    delete it;
    return ret;
  }
  bool NoSQLSupport::ExistData(const BaseBufferSupport &key) {
    //先查询是否有对应的数据
    std::string val;
    MonadStatus status = RawGet(key.ToString(), &val);
    return status.ok();
  }
}//namespace monad
