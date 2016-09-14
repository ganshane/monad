#ifndef MONAD_SDK_IMPL_H_
#define MONAD_SDK_IMPL_H_

#include "leveldb/db.h"
#include <regex>

#include "monad_sdk_code.h"

namespace monad{
  class MonadSDK{
  public:
    MonadSDK(const char* path);
    MONAD_CODE PutCollection(uint32_t region_id,const char* data,const size_t size);
    MONAD_CODE PutId(const char* id_card ,size_t size);
    bool ContainId(const char *id_card,size_t size);
    static leveldb::Status Destroy(const char* path);
    virtual ~MonadSDK();

  private:
    uint32_t CalculateDays(std::smatch& results);
    leveldb::DB* db;
    time_t y1900_time;
  };
}
#endif //MONAD_SDK_IMPL_H_
