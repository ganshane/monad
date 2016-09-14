#include <regex>
#include <iostream>
#include <roaring/roaring.h>
#include "monad_sdk_impl.h"
#include "util/coding.h"

namespace monad{
  const char* SFZH_PATTERN ="([\\d]{6})([\\d]{4})([\\d]{2})([\\d]{2})([\\d]{3})([\\dXx])";
  const std::regex PATTERN(SFZH_PATTERN);


  MonadSDK::MonadSDK(char *path) {
    leveldb::Options options;
    options.create_if_missing = true;
    leveldb::Status status = leveldb::DB::Open(options, path, &db);
  }
  MonadSDK::~MonadSDK() {
    if(db)
      delete db;
  }
  MONAD_CODE MonadSDK::PutCollection(uint32_t region_id, const char *data, const size_t size) {
    leveldb::WriteOptions options;
    char key_data[4];
    leveldb::EncodeFixed32(key_data,region_id);
    leveldb::Slice key(key_data,4);
    leveldb::Slice value(data,size);
    leveldb::Status status = db->Put(options,key,value);
    if(status.ok())
      return MONAD_OK;
    else
      return MONAD_FAIL_PUT_COLLECTION;
  }
  MONAD_CODE MonadSDK::PutId(char *id_card, size_t size) {
    std::string id(id_card,size);
    std::smatch result;
    bool match = std::regex_search(id, result, PATTERN);
    if(match)
    {
      uint32_t region_id = (uint32_t) std::stoi(result[1]);
      uint32_t year = (uint32_t) std::stoi(result[2]);
      uint32_t month = (uint32_t) std::stoi(result[3]);
      uint32_t day = (uint32_t) std::stoi(result[4]);
      uint32_t seq = (uint32_t) std::stoi(result[5]);
      struct tm birth = {0};
      birth.tm_hour = 0;   birth.tm_min = 0; birth.tm_sec = 0;
      birth.tm_year = (year - 1900); birth.tm_mon = month - 1; birth.tm_mday = day;
      //TODO 1900 as constants
      struct tm y1900={0};
      y1900.tm_hour = 0;   y1900.tm_min = 0; y1900.tm_sec = 0;
      y1900.tm_year = 0; y1900.tm_mon = 0; y1900.tm_mday = day;

      uint32_t seconds = (uint32_t) difftime(mktime(&birth),mktime(&y1900));
      uint32_t days = seconds/60 / 60 / 24;
      days |= (seq << 16);

      leveldb::ReadOptions options;

      char key_data[4];
      leveldb::EncodeFixed32(key_data,region_id);
      leveldb::Slice key(key_data,4);
      std::string value;
      leveldb::Status status = db->Get(options,key,&value);
      roaring_bitmap_t *bitmap;
      if(status.ok()){
        bitmap = roaring_bitmap_portable_deserialize(value.c_str());
      }else{
        bitmap = roaring_bitmap_create();
      }
      roaring_bitmap_add(bitmap,days);
      uint32_t expectedsize = (uint32_t) roaring_bitmap_portable_size_in_bytes(bitmap);
      char *serializedbytes = (char *) malloc(expectedsize);
      roaring_bitmap_portable_serialize(bitmap, serializedbytes);
      this->PutCollection(region_id,serializedbytes,expectedsize);
      roaring_bitmap_free(bitmap);
      return MONAD_OK;
    }else{
      return MONAD_WRONG_ID_NUM;
    }
  }
  bool MonadSDK::ContainId(char *id_card, size_t size) {
    std::string id(id_card,size);
    std::smatch result;
    bool match = std::regex_search(id, result, PATTERN);
    if(match){
      uint32_t region_id = (uint32_t) std::stoi(result[1]);
      char key_data[4];
      leveldb::EncodeFixed32(key_data,region_id);
      leveldb::Slice key(key_data,4);
      std::string value;
      leveldb::ReadOptions options;
      leveldb::Status status = db->Get(options,key,&value);

      if(!status.ok())
        return false;

      roaring_bitmap_t* bitmap = roaring_bitmap_portable_deserialize(value.c_str());

      //TODO 整合代码
      uint32_t year = (uint32_t) std::stoi(result[2]);
      uint32_t month = (uint32_t) std::stoi(result[3]);
      uint32_t day = (uint32_t) std::stoi(result[4]);
      uint32_t seq = (uint32_t) std::stoi(result[5]);
      struct tm birth = {0};
      birth.tm_hour = 0;   birth.tm_min = 0; birth.tm_sec = 0;
      birth.tm_year = (year - 1900); birth.tm_mon = month - 1; birth.tm_mday = day;
      //TODO 1900 as constants
      struct tm y1900={0};
      y1900.tm_hour = 0;   y1900.tm_min = 0; y1900.tm_sec = 0;
      y1900.tm_year = 0; y1900.tm_mon = 0; y1900.tm_mday = day;

      uint32_t seconds = (uint32_t) difftime(mktime(&birth),mktime(&y1900));
      uint32_t days = seconds/60 / 60 / 24;
      days |= (seq << 16);
      return roaring_bitmap_contains(bitmap,days);
    }else{
      return false;
    }
  }

  leveldb::Status MonadSDK::Destroy(char* path) {
    leveldb::Options options;
    options.create_if_missing = true;
    return DestroyDB(path, options);
  }
}

