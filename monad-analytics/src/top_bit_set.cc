// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "top_bit_set.h"

#include <assert.h>

#include "top_bit_set_iterator.h"

namespace monad {

  bool TopDocLessThanByFreq(TopDoc* a,TopDoc* b){
    return a->freq <= b->freq;
  };
  bool TopDocLessThanByDoc(TopDoc* a,TopDoc* b){
    return a->doc <= b->doc;
  };

  TopDoc* TopBitSet::Get(uint32_t index) {
    return (index >= _size) ? NULL : _docs[index];
  }

  void TopBitSet::Add(TopDoc* doc, uint32_t index) {
    assert(index < _size);
    _docs[index] = doc;
  }

  TopBitSetIterator TopBitSet::Iterator() {
    return TopBitSetIterator(this);
  }
}//namespace monad
