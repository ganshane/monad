#ifndef MONAD_COLLECTION_INFO_H_
#define MONAD_COLLECTION_INFO_H_
#include "bit_set_wrapper.h"
#include "roaring_bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"

//using namespace emscripten;
namespace monad {
  enum class IdCategory {
    Person =1,
    Car =2 ,
    Mobile =3 ,
    Mac = 4,
    QQ = 5,
    WeiXin =6
  };
  class CollectionInfo{
      RoaringBitSetWrapper* _wrapper;
      TopBitSetWrapper* _top_wrapper;
    //此标记注明_wrapper是否为临时创建的wrapper
      bool _tmp_wrapper_flag;
  public:
    CollectionInfo(RoaringBitSetWrapper* wrapper):
      _wrapper(wrapper),_top_wrapper(NULL),_tmp_wrapper_flag(false){}
    CollectionInfo(TopBitSetWrapper* top_wrapper):
      _wrapper(NULL),_top_wrapper(top_wrapper),_tmp_wrapper_flag(true){}
    virtual ~CollectionInfo();
    RoaringBitSetWrapper* GetOrCreateBitSetWrapper();
    TopBitSetWrapper* GetTopBitSetWrapper();
    void ClearBitSetWrapper();
    bool IsTopCollection();
    RegionDoc** TopWrapper(int32_t n,int32_t& data_len);
    RegionTopDoc** TopWrapperTop(int32_t n,int32_t& data_len);
    int32_t BitCount();
  };
}
#endif //MONAD_COLLECTION_INFO_H_
