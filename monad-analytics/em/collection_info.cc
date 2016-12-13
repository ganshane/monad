#include "collection_info.h"

#include "roaring_bit_set_wrapper.h"

namespace monad {
  CollectionInfo::~CollectionInfo(){
    if(_wrapper){
      delete _wrapper;
      _wrapper = NULL;
    }
    if(_top_wrapper){
      delete _top_wrapper;
      _top_wrapper = NULL;
    }
  }
  RoaringBitSetWrapper* CollectionInfo::GetOrCreateBitSetWrapper(){
    if(!_wrapper)
      _wrapper = RoaringBitSetWrapper::FromTopBitSetWrapper(_top_wrapper);

    return _wrapper;
  }
  TopBitSetWrapper* CollectionInfo::GetTopBitSetWrapper(){
    return _top_wrapper;
  }
  void CollectionInfo::ClearBitSetWrapper(){
    if(_tmp_wrapper_flag && _wrapper) {
      delete _wrapper;
      _wrapper = NULL;
    }
  }
  bool CollectionInfo::IsTopCollection(){
    return _tmp_wrapper_flag;
  }
  RegionDoc** CollectionInfo::TopWrapper(int32_t n,int32_t& data_len){
    if(_wrapper)
      return _wrapper->Top(n,data_len);
    else
      return NULL;
  }
  RegionTopDoc** CollectionInfo::TopWrapperTop(int32_t n,int32_t& data_len){
    if(_top_wrapper)
      return _top_wrapper ->Top(n,data_len);
    else
      return NULL;
  }
  int32_t CollectionInfo::BitCount(){
    if(IsTopCollection()) return _top_wrapper->BitCount();
    else return _wrapper->BitCount();
  }
}