// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_STORAGE_NOSQL_SUPPORT_H_
#define MONAD_STORAGE_NOSQL_SUPPORT_H_
#include <vector>
#include "monad_config.h"
#ifdef MONAD_HAVE_ROCKSDB
#include "rocksdb/db.h"
#include "rocksdb/options.h"
#include "rocksdb/slice.h"
#include "rocksdb/status.h"
#else
#include "leveldb/db.h"
#include "leveldb/options.h"
#include "leveldb/slice.h"
#include "leveldb/status.h"
#endif

#include "monad.h"
#include "nosql_kv_def.h"

namespace monad {
  //回调函数定义接口
  class IteratorCallback {
  public:
    virtual MonadStatus doInIterator(leveldb::Iterator *it) = 0;
    virtual ~IteratorCallback() {
    }
  };
  /*
   *
   * 基础的NoSQL数据库支持,提供基本的NoSQL操作
   */
  class NoSQLSupport {
  private:
    //NoSQL的配置信息
    leveldb::Options _options;
    uint32_t _data_stat_count;
    friend class Transaction;
  protected:
    //database
    leveldb::DB *_db;
    //最大的日志队列
    uint64_t _log_queue_size;
    
  public:
    //初始化Nosql
    NoSQLSupport(const char *db_path, const NoSQLOptions &options) throw(monad::MonadStatus);
    virtual ~NoSQLSupport();
    //原始的获取数据，key不进行任何修饰
    MonadStatus RawGet(const leveldb::Slice &key, std::string *value);
    MonadStatus RawGet(BaseBufferSupport &key, std::string *value);
    //原始的获取数据，key不进行任何修饰
    MonadStatus RawPut(const leveldb::Slice &key, const leveldb::Slice &val);
    //原始的获取数据，key不进行任何修饰
    MonadStatus RawPut(BaseBufferSupport &key, const leveldb::Slice &val);
    MonadStatus doInIterator(IteratorCallback &callback);
    /**
     * 判断某一key是否存在
     * @param key 数据库的键
     * @return true 存在数据,false 不存在数据
     */
    bool ExistData(const BaseBufferSupport &key);
    /**
     * 统计某一范围内的数据数目
     * @param from 起始位置，包含此数据
     * @param until 终止位置，不包含此数据
     * @return 返回的数据总数
     */
    uint32_t Count(const leveldb::Slice &from, const leveldb::Slice &until);
    std::string *FindMaxKeyInRange(const leveldb::Slice &from,
                                   const leveldb::Slice &until) throw(MonadStatus);
    std::string *FindMinKeyInRange(const leveldb::Slice &from,
                                   const leveldb::Slice &until) throw(MonadStatus);
    uint32_t Count(const BaseBufferSupport &from, const BaseBufferSupport &until) {
      return Count(from.ToString(), until.ToString());
    }
  };
  class SlaveNoSQLSupport: public NoSQLSupport {
  private:
    uint32_t _data_count;
  public:
    /**
     * 构造MatcherNoSQL实例
     * @param db_path 数据库路径
     * @param options 数据库的参数
     */
    SlaveNoSQLSupport(const char *db_path, const NoSQLOptions &options)
    throw(monad::MonadStatus)
    : NoSQLSupport(db_path, options) {
      _data_count = 0;
      SlaveDataCountKey key;
      std::string value;
      MonadStatus status = RawGet(key, &value);
      if (status.ok()) {
        SlaveDataCountValue data_count_value(value);
        _data_count = data_count_value.Count();
      }
    };
    /**
     * 放入binlog日志
     * @param binlog_value 同步的值
     * @param data 原始数据,如果操作对应为DEL,那么此项数据为空
     * @return 同步的结果
     */
    virtual MonadStatus PutBinlog(const SyncBinlogValue &binlog_value);
    virtual MonadStatus PutSlaveBinlog(const SlaveBinlogValue &binlog_value, const leveldb::Slice &data);
    uint32_t GetDataCount() {
      return _data_count;
    }
    /**
     * 发现下一个binlog日志
     * @param binlog_seq 下一个binlog日志序列
     * @return binlog日志
     */
    SyncBinlogValue *FindNextBinlog(uint64_t binlog_seq);
    /**
     * 查找最后一次binlog的序号
     */
    uint64_t FindLastBinlog();
    /**
     * 删除从from位置的binlog日志，到结束点的日志
     */
    MonadStatus DeleteBinlogRange(const uint64_t from, const uint64_t to);
  };
}//namespace monad
#endif //MONAD_STORAGE_NOSQL_SUPPORT_H_
