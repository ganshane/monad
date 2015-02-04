// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_STORAGE_NOSQL_KV_DEF_H_
#define MONAD_STORAGE_NOSQL_KV_DEF_H_

#include "monad_config.h"

#ifdef MONAD_HAVE_ROCKSDB
#include "rocksdb/status.h"
#else
#include "leveldb/status.h"
#endif


#include "monad.h"
#include "blizzard_hash.h"
#include "coding.h"

namespace monad {
  
  class BaseBufferSupport : private Uncopyable {
  public:
    inline const std::string &ToString() const {
      return _buf;
    }
    inline bool operator<(const BaseBufferSupport &rht) {
      return _buf < rht._buf;
    }
    inline bool operator>(const BaseBufferSupport &rht) {
      return _buf > rht._buf;
    }
  protected:
    std::string _buf;
  };
  class NoSQLKey: public BaseBufferSupport {
  public:
    inline NoSQLKey(const DataType data_type, const leveldb::Slice &key) {
      _buf.push_back(data_type);
      _buf.append(key.data(), key.size());
    }
    inline NoSQLKey(const leveldb::Slice &key) {
      _buf.append(key.data(), key.size());
    }
    inline std::string Key() {
      return std::string(_buf, 1,_buf.size() - 1);
    }
  };
  class NormalSeqDataKey: public BaseBufferSupport {
  public:
    inline NormalSeqDataKey(const uint8_t partition_id, const uint32_t data_seq) {
      _buf.push_back(KV);
      _buf.push_back(partition_id);
      EncodeFixed32WithBigEndian(_buf, data_seq);
    }
    inline NormalSeqDataKey(const leveldb::Slice &key) {
      _buf.append(key.data(), key.size());
    }
    uint32_t DataSeq() {
      return DecodeFixed32WithBigEndian(_buf.c_str() + 2);
    }
  };
  class SlaveBinlogKey: public BaseBufferSupport {
  public:
    inline SlaveBinlogKey(const uint64_t seq) {
      _buf.push_back(BINLOG);
      EncodeFixed64WithBigEndian(_buf, seq);
    };
    inline SlaveBinlogKey(const leveldb::Slice &data) {
      _buf.append(data.data(), data.size());
    };
    inline uint64_t Seq() const {
      return DecodeFixed64WithBigEndian(_buf.c_str() + 1);
    };
  };
  class SlaveBinlogValue: public BaseBufferSupport {
  public:
    inline SlaveBinlogValue(const uint8_t paritition_id,
                            const uint64_t seq,
                            const DataCommandType command_type,
                            const BaseBufferSupport &key) {
      _buf.push_back(paritition_id);
      EncodeFixed64(_buf, seq);
      _buf.push_back(command_type);
      std::string key_str = key.ToString();
      EncodeFixed32(_buf, key_str.length());
      _buf.append(key.ToString());
    };
    inline SlaveBinlogValue(const leveldb::Slice &data) {
      _buf.append(data.data(), data.size());
    };
    inline uint8_t PartitionId() const {
      return _buf[0];
    }
    inline uint64_t Seq() const {
      return DecodeFixed64(_buf.c_str() + 1);
    };
    inline DataCommandType CommandType() const {
      return DataCommandType(_buf[9]);
    };
    inline uint32_t KeyLength() const {
      return DecodeFixed32(_buf.c_str() + 10);
    }
    inline std::string Key() const {
      uint32_t key_len = KeyLength();
      uint32_t key_start_pos = 10 + sizeof(uint32_t);
      return std::string(_buf, key_start_pos, key_len);
    };
    inline DataType GetDataType() const{
      uint32_t key_start_pos = 10 + sizeof(uint32_t);
      return DataType(_buf[key_start_pos]);
    }
  };
  class SyncBinlogKey: public BaseBufferSupport {
  public:
    inline SyncBinlogKey(const uint8_t partition_id, const uint64_t seq) {
      _buf.push_back(BINLOG);
      _buf.push_back(partition_id);
      EncodeFixed64WithBigEndian(_buf, seq);
    };
    inline SyncBinlogKey(const leveldb::Slice &data) {
      _buf.append(data.data(), data.size());
    };
    inline uint8_t PartitionId() const {
      return _buf[1];
    }
    inline uint64_t Seq() const {
      return DecodeFixed64WithBigEndian(_buf.c_str() + 2);
    };
  };
  class SyncBinlogValue: public SlaveBinlogValue {
  public:
    inline SyncBinlogValue(const uint8_t partition_id,
                           const uint64_t seq,
                           const DataCommandType command_type,
                           const BaseBufferSupport &key,
                           const leveldb::Slice &value)
    : SlaveBinlogValue(partition_id, seq, command_type, key) {
      //如果有数据，则存入数据
      if (value.size() > 0)
        _buf.append(value.ToString());
    };
    inline SyncBinlogValue(const leveldb::Slice &data): SlaveBinlogValue(data) {
    };
    inline std::string Value() const {
      uint32_t key_len = KeyLength();
      uint32_t value_start_pos = 10 + sizeof(uint32_t) + key_len;
      return std::string(_buf, value_start_pos, _buf.size() - value_start_pos);
    };
    inline std::string ToSlaveBinlogString() const {
      uint32_t key_len = KeyLength();
      uint32_t value_start_pos = 10 + sizeof(uint32_t) + key_len;
      return std::string(_buf, 0, value_start_pos);
    }
  };
  
  //依据卡号信息对数据进行分区
  class CardPartitionMappingKey: public BaseBufferSupport {
  private:
    int64_t key_hash;
  public:
    inline CardPartitionMappingKey(const int64_t card_id, const uint32_t data_type) {
      char str[64];
      memset(str, 0, sizeof(str));
      sprintf(str, MONAD_F_U64, card_id);
      //EncodeFixed64WithBigEndian(str, card_id);
      key_hash = BlizzardHash::HashString(str);
      _buf.push_back(PARTITION_INFO);
      EncodeFixed64WithBigEndian(_buf, key_hash);
      EncodeFixed32(_buf, data_type);
    };
    inline int64_t KeyHash() {
      return key_hash;
    }
  };
  //PARTITION_INFO+KH
  class PartitionMappingKey: public BaseBufferSupport {
  public:
    inline PartitionMappingKey(const leveldb::Slice &key) {
      int64_t key_hash = BlizzardHash::HashString(key.ToString());
      _buf.push_back(PARTITION_INFO);
      EncodeFixed64WithBigEndian(_buf, key_hash);
    };
  };
  //PartitionId+DATA_SEQ
  class PartitionMappingValue: public BaseBufferSupport {
  public:
    inline PartitionMappingValue(uint8_t partition_id) {
      _buf.push_back(partition_id);
    }
    inline PartitionMappingValue(uint8_t partition_id, uint32_t data_seq) {
      _buf.push_back(partition_id);
      EncodeFixed32WithBigEndian(_buf, data_seq);
    }
    inline PartitionMappingValue(const leveldb::Slice &val) {
      _buf.append(val.data(), val.size());
    }
    inline uint8_t PartitionId() {
      return _buf[0];
    }
    inline uint32_t DataSeq() {
      return DecodeFixed32WithBigEndian(_buf.c_str() + 1);
    }
  };
  //同步服务器分区数据
  class SlaveDataCountKey: public BaseBufferSupport {
  public:
    inline SlaveDataCountKey() {
      static char data_count_key_prefix[20] = "_data_count";
      _buf.append(data_count_key_prefix);
    }
  };
  class SlaveDataCountValue: public BaseBufferSupport {
  public:
    inline SlaveDataCountValue(const leveldb::Slice &slice) {
      _buf.assign(slice.data(), slice.size());
    }
    inline SlaveDataCountValue(const uint32_t count) {
      EncodeFixed32(_buf, count);
    }
    inline uint32_t Count() {
      return DecodeFixed32(_buf.data());
    }
  };
  //同步服务器分区数据序列
  class SyncPartitionDataSeqKey: public BaseBufferSupport {
  public:
    inline SyncPartitionDataSeqKey(const uint8_t partition_id) {
      static char data_seq_key_prefix[20] = "_data_seq";
      _buf.append(data_seq_key_prefix);
      _buf.push_back(partition_id);
    }
  };
  class SyncPartitionDataSeqValue: public BaseBufferSupport {
  public:
    inline SyncPartitionDataSeqValue(const leveldb::Slice &slice) {
      _buf.assign(slice.data(), slice.size());
    }
    inline SyncPartitionDataSeqValue(const uint32_t count) {
      EncodeFixed32(_buf, count);
    }
    inline uint32_t Seq() {
      return DecodeFixed32(_buf.data());
    }
  };
  //同步服务器分区数据
  class SyncPartitionDataCountKey: public BaseBufferSupport {
  public:
    inline SyncPartitionDataCountKey(const uint8_t partition_id, const uint32_t data_type) {
      static char data_count_key_prefix[20] = "_data_count";
      _buf.append(data_count_key_prefix);
      _buf.push_back(partition_id);
      EncodeFixed32(_buf, data_type);
    }
  };
  class SyncPartitionDataCountValue: public BaseBufferSupport {
  public:
    inline SyncPartitionDataCountValue(const leveldb::Slice &slice) {
      _buf.assign(slice.data(), slice.size());
    }
    inline SyncPartitionDataCountValue(const uint32_t count) {
      EncodeFixed32(_buf, count);
    }
    inline uint32_t Count() {
      return DecodeFixed32(_buf.data());
    }
  };
  class MetaPartitionDataCountKey: public BaseBufferSupport {
  public:
    inline MetaPartitionDataCountKey(const uint8_t partition_id) {
      static char data_count_key_prefix[20] = "_data_count";
      _buf.append(data_count_key_prefix);
      _buf.push_back(partition_id);
    }
  };
  class MetaPartitionDataCountValue: public BaseBufferSupport {
  public:
    inline MetaPartitionDataCountValue(const leveldb::Slice &slice) {
      _buf.assign(slice.data(), slice.size());
    }
    inline MetaPartitionDataCountValue(const uint32_t count) {
      EncodeFixed32(_buf, count);
    }
    inline uint32_t Count() {
      return DecodeFixed32(_buf.data());
    }
  };
  //DataTimestampKey
  class DataTimestampKey: public BaseBufferSupport {
  public:
    inline DataTimestampKey(const uint32_t data_type) {
      static char data_timestamp_key_prefix[20] = "_data_timestamp_";
      _buf.append(data_timestamp_key_prefix);
      EncodeFixed32WithBigEndian(_buf, data_type);
    };
  };
  //DataTimestampValue
  class DataTimestampValue: public BaseBufferSupport {
  public:
    inline DataTimestampValue(const leveldb::Slice &slice) {
      _buf.append(slice.data(), slice.size());
    };
    inline DataTimestampValue(const uint64_t timestamp) {
      EncodeFixed64WithBigEndian(_buf, timestamp);
    }
    inline uint64_t Timestamp() {
      return DecodeFixed64WithBigEndian(_buf.c_str());
    }
  };
}//namespace monad

#endif //MONAD_STORAGE_NOSQL_KV_DEF_H_
