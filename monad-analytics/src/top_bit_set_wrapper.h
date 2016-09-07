// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_TOP_BIT_SET_WRAPPER_H_
#define MONAD_TOP_BIT_SET_WRAPPER_H_

#include <vector>

#include "bit_set_region.h"
#include "top_bit_set.h"

namespace monad{
  template<typename T>
  struct BitSetRegion;
  template<typename WRAPPER,typename BIT_SET>
  class BitSetWrapperIterator;
  class OpenBitSetWrapper;
  class SparseBitSet;
  class SparseBitSetWrapper;
  class RoaringBitSet;
  class RoaringBitSetWrapper;
  template<typename WRAPPER,typename BIT_SET>
  class BitSetWrapper;

  class TopBitSetWrapper{
  public:
    //bool FastGet(int32_t index);
    int32_t Size(){
      return _data.size();
    };
    int32_t BitCount();
    monad::RegionTopDoc** Top(int32_t n,int32_t& data_len);
    virtual ~TopBitSetWrapper();
    void Commit();
    float elapsed_time;
  private:
    typedef BitSetWrapperIterator<TopBitSetWrapper,TopBitSet> TWI;
    std::vector<BitSetRegion<TopBitSet>*> _data;
    uint32_t _total_doc;
    BitSetWrapperIterator<TopBitSetWrapper,TopBitSet>* Iterator();
    friend class OpenBitSetWrapper;
    friend class SparseBitSetWrapper;
    friend class RoaringBitSetWrapper;
    friend class BitSetWrapperIterator<TopBitSetWrapper,TopBitSet>;
    friend class BitSetWrapper<OpenBitSetWrapper,OpenBitSet>;
    friend class BitSetWrapper<SparseBitSetWrapper,SparseBitSet>;
    friend class BitSetWrapper<RoaringBitSetWrapper,RoaringBitSet>;
  }; //class TopBitSetWrapper
} //namespace monad
#endif //MONAD_TOP_BIT_SET_WRAPPER_H_
