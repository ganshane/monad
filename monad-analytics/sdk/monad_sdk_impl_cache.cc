#include <iostream>
#include "monad_sdk_impl.h"

#include "uthash.h"

namespace monad {
  struct CacheEntry {
      uint32_t region_id;
      roaring_bitmap_t* bitmap;
      UT_hash_handle hh;
  };
  struct CacheEntry *cache = NULL;

  static uint32_t cache_ram_size=0;

  void free_cache_item(struct CacheEntry *entry){
    roaring_bitmap_free(entry->bitmap);
    free(entry);
  }
  void remove_cache_entry(CacheEntry *entry) {
    if (entry) {
      // remove it (so the subsequent add will throw it on the front of the list)
      HASH_DELETE(hh, cache, entry);
      cache_ram_size -= roaring_bitmap_portable_size_in_bytes(entry->bitmap);
      free_cache_item(entry);
      std::cout << "RC,count:" << HASH_COUNT(cache) << " ram :" << cache_ram_size <<" " << std::endl;
    }
  }
  roaring_bitmap_t* find_in_cache(uint32_t key)
  {
    struct CacheEntry *entry;
    HASH_FIND_INT(cache, &key, entry);
    if (entry) {
      // remove it (so the subsequent add will throw it on the front of the list)
      HASH_DELETE(hh, cache, entry);
      HASH_ADD_INT(cache,region_id, entry);
      return entry->bitmap;
    }
    return NULL;
  }

  void add_to_cache(uint32_t region_id, roaring_bitmap_t* value,uint32_t max_cache_ram)
  {
    struct CacheEntry *entry, *tmp_entry;
    entry = (CacheEntry *) malloc(sizeof(struct CacheEntry));
    entry->region_id = region_id;
    entry->bitmap = value;
    size_t new_size = roaring_bitmap_portable_size_in_bytes(value);
    /*
     * disable optimize
    size_t old_size = new_size;
    if(roaring_bitmap_run_optimize(value)){
      new_size = roaring_bitmap_portable_size_in_bytes(value);
      std::cout << "compressed from " << old_size << " to " << new_size << " bit count"<< roaring_bitmap_get_cardinality(value) <<std::endl;
    }
     */

    HASH_ADD_INT(cache, region_id, entry);
    cache_ram_size += new_size;

    // prune the cache to MAX_CACHE_SIZE
    if (cache_ram_size > max_cache_ram) {
      HASH_ITER(hh, cache, entry, tmp_entry) {
        remove_cache_entry(entry);
        break;
      }
    }
    std::cout << "AC, count:" << HASH_COUNT(cache) << " ram :" << cache_ram_size <<" " << " r: "<<region_id << " c:" << roaring_bitmap_get_cardinality(value) << std::endl;
  }
  void MonadSDK::AddCache(uint32_t region_id, roaring_bitmap_t *value) {
    add_to_cache(region_id,value,max_cache_ram);
  }
  roaring_bitmap_t * MonadSDK::GetBitmapFromCache(uint32_t region_id) {
    return find_in_cache(region_id);
  }

  void MonadSDK::ClearCache() {
    struct CacheEntry *entry, *tmp_entry;
    HASH_ITER(hh, cache, entry, tmp_entry) {
      remove_cache_entry(entry);
    }
  }

  void MonadSDK::RemoveCache(uint32_t region_id) {
    struct CacheEntry *entry;
    HASH_FIND_INT(cache, &region_id, entry);
    remove_cache_entry(entry);
  }
}