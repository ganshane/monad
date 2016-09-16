#ifndef MONAD_SDK_IMPL_H_
#define MONAD_SDK_IMPL_H_

#include <regex>
#include "leveldb/db.h"
#include "roaring/roaring.h"

#include "monad_sdk_code.h"

namespace monad{
  struct CacheEntry;
  class MonadSDK{
  public:
    MonadSDK(const char* path,const uint32_t cache_ram=50 * 1024 * 1024);
    MONAD_CODE PutCollection(uint32_t region_id,const char* data,const size_t size);
    MONAD_CODE PutId(const char* id_card ,size_t size);
    MONAD_CODE PutKV(const leveldb::Slice& key,const leveldb::Slice& value);
    MONAD_CODE GetKV(const leveldb::Slice& key,std::string* value);
    bool ContainId(const char *id_card,size_t size);
    static leveldb::Status Destroy(const char* path);
    virtual ~MonadSDK();
    //only for test
    MONAD_CODE CalculateDays(const char* id_card,const size_t size,uint32_t& days,uint32_t& region_id);

  private:
    void AddCache(uint32_t region_id,roaring_bitmap_t* value);
    void RemoveCache(uint32_t region_id);
    roaring_bitmap_t* GetBitmapFromCache(uint32_t region_id);
    void ClearCache();

    leveldb::DB* db;
    uint32_t y1900_days;
    uint32_t max_cache_ram;
  };
}
#endif //MONAD_SDK_IMPL_H_
