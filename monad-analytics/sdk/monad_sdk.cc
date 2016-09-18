#include "monad_sdk.h"

#include <stdint.h>
#include <util/coding.h>

#include "monad_sdk_impl.h"

using monad::MonadSDK;

extern "C" {
/**
 * 根据给定的handle以及文件路径创建
 * @param handle 对象的handle
 * @param db_path 数据写入的文件路径
 * @param cache_ram 支持的缓存内存大小
 */
MONAD_CODE monad_coll_create(void **handle, const char *db_path, const uint32_t cache_ram) {
  *handle = new MonadSDK(db_path, cache_ram);
  return MONAD_OK;
}
/**
 * 放入某一数据,此数据通常是分割后的,方便从网络进行下载
 * @param handle 操作对象
 * @param data 分段数据
 * @param size 分段数据的大小
 */
void monad_coll_put_seg(void *handle, const char *data,const  size_t size) {
  uint32_t region_id = leveldb::DecodeFixed32(data);
  MonadSDK *sdk = (MonadSDK *) handle;
  sdk->PutCollection(region_id, data + 4, size - 4);
}
/**
 * 是否包含给定的对象ID
 * @param handle 操作的handle对象
 * @param id_card 待操作的对象ID
 * @param size 对象
 */
bool monad_coll_contain_id(void *handle, const char *id_card, const size_t size) {
  MonadSDK *sdk = (MonadSDK *) handle;
  return sdk->ContainId(id_card, size);
}

/**
 * 放入对象ID到容器中
 * @param handle 操作的handle对象
 * @param id_card 待放入的对象ID
 * @param size 对象ID的长度
 */
MONAD_CODE monad_coll_put_id(void *handle,const  char *id_card, const size_t size) {
  MonadSDK *sdk = (MonadSDK *) handle;
  return sdk->PutId(id_card, size);
}
/**
 * 放入kv数据
 */
MONAD_CODE monad_coll_put_kv(void* handle,const char* key,const size_t key_size,const char* value,size_t value_size){
  MonadSDK *sdk = (MonadSDK *) handle;
  sdk->PutKV(leveldb::Slice(key,key_size),leveldb::Slice(value,value_size));
}
static char* CopyString(const std::string& str) {
  char* result = reinterpret_cast<char*>(malloc(sizeof(char) * str.size()));
  memcpy(result, str.data(), sizeof(char) * str.size());
  return result;
}
MONAD_EXPORT MONAD_CODE monad_coll_get_kv(void* handle,const char* key,const size_t key_size,char** value,size_t* value_size){
  MonadSDK *sdk = (MonadSDK *) handle;
  std::string dest;
  MONAD_CODE  ret = sdk->GetKV(leveldb::Slice(key,key_size),&dest);
  if(ret == MONAD_OK){
    *value_size= dest.size();
    *value = CopyString(dest);
  }else{
    *value_size = 0;
  }
  return ret;
}
/**
 * 释放内存空间,关闭数据库
 */
void monad_coll_release(void *handle) {
  MonadSDK *sdk = (MonadSDK *) handle;
  delete sdk;
}
}
