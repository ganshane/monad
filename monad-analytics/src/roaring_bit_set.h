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

    BitSetIterator* ToIterator() {}
    void And(const RoaringBitSet& other) {}
    void Or(const RoaringBitSet& other)  {}
    void Remove(const RoaringBitSet& other) {}
    int32_t Weight() {};
    BitSet<RoaringBitSet>* Clone() {};

  private:
    roaring_bitmap_t * _underlying;

  };
}
#endif //ROARING_BIt_SET_H_
