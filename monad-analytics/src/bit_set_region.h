// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_REGION_H_
#define MONAD_OPEN_BIT_SET_REGION_H_

#include "open_bit_set.h"

namespace monad{
  /**
   * 记录集合区域的结构
   */
  template<typename T>
  struct BitSetRegion{
    uint32_t region;//BitSet所属区域
    T* bit_set; //BitSet对象
  public:
    BitSetRegion(){
      region = 0;
      bit_set = NULL;
    };
    ~BitSetRegion(){
      if(bit_set){
        delete bit_set;
      }
    };
  };
  //对BitSet按照区域进行排序
  template<typename T>
  bool SortBitSetRegion(BitSetRegion<T>* a, BitSetRegion<T>* b) {
    return a->region <= b->region;
  };
}//namespace monad
#endif //MONAD_OPEN_BIT_SET_REGION_H_
