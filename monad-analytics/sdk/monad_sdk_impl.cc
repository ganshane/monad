#include <iostream>
#include <stdio.h>
#include <string>
#include <sstream>

#include <roaring/roaring.h>
#include <leveldb/cache.h>
#include "monad_sdk_impl.h"
#include "util/coding.h"

namespace monad{
  const char* SFZH_PATTERN ="([\\d]{6})([\\d]{4})([\\d]{2})([\\d]{2})([\\d]{3})([\\dXx])";
  const std::regex PATTERN(SFZH_PATTERN);

  /**
   * Convert your dates to integer denoting the number of days since an epoch, then subtract. In this example i chosed Rata Die, an explanation of the algorithm can be found at <http://mysite.verizon.net/aesir_research/date/rata.htm>.
   * http://stackoverflow.com/questions/14218894/number-of-days-between-two-dates-c
   */
  static inline int rdn(int y, int m, int d) { /* Rata Die day one is 0001-01-01 */
    if (m < 3)
      y--, m += 12;
    return 365*y + y/4 - y/100 + y/400 + (153*m - 457)/5 + d - 306;
  }

  static uint32_t convert(std::string str){
    return atoi(str.c_str());
    /*
    return std::stoi(str);
    std::stringstream stream(str);
    uint32_t  result;
    stream >> result;
    return result;
     */
  }

  static inline leveldb::Slice CreateRegionKey(uint32_t region_id,char* key_data){
    leveldb::EncodeFixed32(key_data,region_id);
    return leveldb::Slice(key_data,4);
  }
  MONAD_CODE MonadSDK::CalculateDays(const char* id_card,const size_t size,uint32_t& days,uint32_t& region_id){
    std::string id(id_card,size);
    std::smatch result;
    bool match = std::regex_search(id, result, PATTERN);
    if(match) {
      region_id = (uint32_t) convert(result[1]);
      uint32_t year = (uint32_t) convert(result[2]);
      uint32_t month = (uint32_t) convert(result[3]);
      uint32_t day = (uint32_t) convert(result[4]);
      uint32_t seq = (uint32_t) convert(result[5]);

      days = (uint32_t) rdn(year, month, day);
      days = (days - y1900_days) | (seq << 16);

      return MONAD_OK;
    }else{
      return MONAD_WRONG_ID_NUM;
    }

  }
  MonadSDK::MonadSDK(const char *path,const uint32_t cache_ram) {
    leveldb::Options options;
    options.block_size = 50 * 1024;
//    options.block_cache = leveldb::NewLRUCache(100 * 1024 * 1024);
    options.create_if_missing = true;
    leveldb::Status status = leveldb::DB::Open(options, path, &db);

    y1900_days = (uint32_t) rdn(1900,1,1);

    max_cache_ram = cache_ram;

  }
  MonadSDK::~MonadSDK() {
    if(db)
      delete db;
    ClearCache();
  }
  MONAD_CODE MonadSDK::PutCollection(uint32_t region_id, const char *data, const size_t size) {
    leveldb::WriteOptions options;
    char key_data[4];
    leveldb::Slice key= CreateRegionKey(region_id,key_data);
    leveldb::Slice value(data,size);
    leveldb::Status status = db->Put(options,key,value);
    if(status.ok()) {
      RemoveCache(region_id);
      return MONAD_OK;
    }
    else
      return MONAD_FAIL_PUT_COLLECTION;
  }
  MONAD_CODE MonadSDK::PutId(const char *id_card, size_t size) {
    uint32_t id_seq(0),region_id(0);
    MONAD_CODE r = CalculateDays(id_card,size,id_seq,region_id);
    if(r == MONAD_OK){

      leveldb::ReadOptions options;

      char key_data[4];
      leveldb::Slice key=CreateRegionKey(region_id,key_data);
      std::string value;
      leveldb::Status status = db->Get(options,key,&value);
      roaring_bitmap_t *bitmap;
      if(status.ok()){
        bitmap = roaring_bitmap_portable_deserialize(value.c_str());
      }else{
        bitmap = roaring_bitmap_create();
      }
      roaring_bitmap_add(bitmap,id_seq);
      uint32_t expectedsize = (uint32_t) roaring_bitmap_portable_size_in_bytes(bitmap);
      char *serializedbytes = (char *) malloc(expectedsize);
      roaring_bitmap_portable_serialize(bitmap, serializedbytes);
      this->PutCollection(region_id,serializedbytes,expectedsize);
      roaring_bitmap_free(bitmap);
      free(serializedbytes);
      RemoveCache(region_id);
      return MONAD_OK;
    }else{
      return MONAD_WRONG_ID_NUM;
    }
  }
  bool MonadSDK::ContainId(const char *id_card, size_t size) {
    uint32_t id_seq(0),region_id(0);
    MONAD_CODE r = CalculateDays(id_card,size,id_seq,region_id);
    if(r == MONAD_OK){
      /*
      clock_t   start,   finish;
      double     duration;
      static double total1=0,total2=0,total3=0;
      start = clock();
       */

      roaring_bitmap_t* bitmap = GetBitmapFromCache(region_id);

      if(bitmap == NULL) {
        char key_data[4];
        leveldb::Slice key = CreateRegionKey(region_id, key_data);
        std::string value;
        leveldb::ReadOptions options;
        options.fill_cache = true;
        leveldb::Status status = db->Get(options, key, &value);

        /*
        finish = clock();
        duration = finish-start;
        total1 += duration;
        std::cout << "total 1:"<< total1 << " " <<duration << std::endl;
         */

        if (!status.ok())
          return false;

        bitmap = roaring_bitmap_portable_deserialize(value.c_str());
        AddCache(region_id,bitmap);
      }
      /*
      finish = clock();
      duration = finish-start;
      total2 += duration;
      std::cout << "total 2:"<<total2-total1 << " " <<duration << std::endl;
       */
      bool r = roaring_bitmap_contains(bitmap,id_seq);
      //don't free ,because the bitmap in cache
//      roaring_bitmap_free(bitmap);
      /*
      finish = clock();
      duration = finish - start;
      total3 += duration;
      std::cout << total3 << " " <<duration << std::endl;
      */

      return r;
    }else{
      return false;
    }
  }

  leveldb::Status MonadSDK::Destroy(const char* path) {
    leveldb::Options options;
    options.create_if_missing = true;
    return DestroyDB(path, options);
  }
  MONAD_CODE MonadSDK::PutKV(const leveldb::Slice &key, const leveldb::Slice &value) {
    leveldb::WriteOptions options;
    leveldb::Status status = db->Put(options,key,value);
    if(status.ok())
      return MONAD_OK;
    else
      return MONAD_FAIL_PUT_KV;
  }
  MONAD_CODE MonadSDK::GetKV(const leveldb::Slice &key, std::string *value) {
    leveldb::ReadOptions options;
    leveldb::Status status = db->Get(options,key,value);
    if(status.ok())
      return MONAD_OK;
    else
      return MONAD_FAIL_GET_KV;
  }
}

