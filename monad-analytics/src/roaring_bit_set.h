// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef ROARING_BIt_SET_H_
#define ROARING_BIt_SET_H_

#include "roaring/roaring.h"

#include <stdint.h>
#include "bit_set.h"

namespace monad{
  class RoaringBitSet:public BitSet<RoaringBitSet>{
  public:
    RoaringBitSet(uint32_t i=1);
    virtual ~RoaringBitSet();
    void Set(uint32_t i);
    bool Get(uint32_t i);
    void Optimize();
    uint32_t GetWordsLength(){
//      return Cardinality();
      return _num_words;
    }
    uint32_t Cardinality(){
      return roaring_bitmap_get_cardinality(_underlying);
    }
    RoaringBitSet* Clone(){
      roaring_bitmap_t* dest = roaring_bitmap_copy(_underlying);
      RoaringBitSet* bitSet = new RoaringBitSet();
      bitSet->_underlying = dest;
      bitSet->_num_words = _num_words;
      return bitSet;
    }

    BitSetIterator* ToIterator();
    void And(const RoaringBitSet& other) {roaring_bitmap_and_inplace(_underlying,other._underlying);}
    void Or(const RoaringBitSet& other)  {roaring_bitmap_or_inplace(_underlying,other._underlying);}
    void Remove(const RoaringBitSet& other) {roaring_bitmap_andnot_inplace(_underlying,other._underlying);}

  private:
    friend class RoaringBitSetIterator;
    roaring_bitmap_t * _underlying;
    uint32_t _num_words;

  };
}
#endif //ROARING_BIt_SET_H_
