#ifndef MONAD_SDK_IMPL_H_
#define MONAD_SDK_IMPL_H_

#include "leveldb/db.h"

#include "monad_sdk_code.h"

namespace monad{
  class MonadSDK{
  public:
    MonadSDK(char* path);
    MONAD_CODE PutCollection(uint32_t region_id,const char* data,const size_t size);
    MONAD_CODE PutId(char* id_card ,size_t size);
    bool ContainId(char* id_card,size_t size);
    static leveldb::Status Destroy(char* path);
    virtual ~MonadSDK();

  private:
    leveldb::DB* db;
  };
}
#endif //MONAD_SDK_IMPL_H_
