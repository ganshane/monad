// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef ROARING_BIt_SET_H_
#define ROARING_BIt_SET_H_

#include "roaring/roaring.h"

#include <stdint.h>
#include "bit_set.h"

namespace monad{
  class RoaringBitSet:BitSet<RoaringBitSet>{
  public:
    RoaringBitSet();
    virtual ~RoaringBitSet();
    void Set(uint32_t i);
    bool Get(uint32_t i);
    void Optimize();
    uint32_t NextSetBit(uint32_t doc);

    BitSetIterator* ToIterator();
    void And(const RoaringBitSet& other) {roaring_bitmap_and_inplace(_underlying,other._underlying);}
    void Or(const RoaringBitSet& other)  {roaring_bitmap_or_inplace(_underlying,other._underlying);}
    void Remove(const RoaringBitSet& other) {roaring_bitmap_andnot_inplace(_underlying,other._underlying);}

  private:
    friend class RoaringBitSetIterator;
    roaring_bitmap_t * _underlying;

  };
}
#endif //ROARING_BIt_SET_H_
