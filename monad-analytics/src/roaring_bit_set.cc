// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "roaring_bit_set.h"

#include "roaring_bit_set_iterator.h"

namespace monad{
  RoaringBitSet::RoaringBitSet() {
    _underlying = roaring_bitmap_create();
  }
  RoaringBitSet::~RoaringBitSet(){
    roaring_bitmap_free(_underlying);
  }
  void RoaringBitSet::Set(uint32_t i) {
    roaring_bitmap_add(_underlying,i);
  }
  bool RoaringBitSet::Get(uint32_t i) {
    return roaring_bitmap_contains(_underlying,i);
  }
  void RoaringBitSet::Optimize() {
    roaring_bitmap_run_optimize(_underlying);
  }
  BitSetIterator* RoaringBitSet::ToIterator() {
    return new RoaringBitSetIterator(this);
  }
}