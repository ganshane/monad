#include <iostream>
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


  static inline leveldb::Slice CreateRegionKey(uint32_t region_id,char* key_data){
    leveldb::EncodeFixed32(key_data,region_id);
    return leveldb::Slice(key_data,4);
  }
  uint32_t MonadSDK::CalculateDays(std::smatch& result){
    uint32_t year = (uint32_t) std::stoi(result[2]);
    uint32_t month = (uint32_t) std::stoi(result[3]);
    uint32_t day = (uint32_t) std::stoi(result[4]);
    uint32_t seq = (uint32_t) std::stoi(result[5]);

    uint32_t days = (uint32_t) rdn(year,month,day);

    return (days - y1900_days) | (seq << 16);
  }
  MonadSDK::MonadSDK(const char *path) {
    leveldb::Options options;
    options.block_size = 50 * 1024;
//    options.block_cache = leveldb::NewLRUCache(100 * 1024 * 1024);
    options.create_if_missing = true;
    leveldb::Status status = leveldb::DB::Open(options, path, &db);

    y1900_days = (uint32_t) rdn(1900,1,1);

    max_cache_ram = 50 * 1024 * 1024;

  }
  MonadSDK::~MonadSDK() {
    if(db)
      delete db;
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
    std::string id(id_card,size);
    std::smatch result;
    bool match = std::regex_search(id, result, PATTERN);
    if(match)
    {
      uint32_t region_id = (uint32_t) std::stoi(result[1]);
      uint32_t id_seq = CalculateDays(result);

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
      return MONAD_OK;
    }else{
      return MONAD_WRONG_ID_NUM;
    }
  }
  bool MonadSDK::ContainId(const char *id_card, size_t size) {
    std::string id(id_card,size);
    std::smatch result;
    bool match = std::regex_search(id, result, PATTERN);
    if(match){
      /*
      clock_t   start,   finish;
      double     duration;
      static double total1=0,total2=0,total3=0;
      start = clock();
       */

      uint32_t region_id = (uint32_t) std::stoi(result[1]);

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
      uint32_t id_seq = CalculateDays(result);
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

