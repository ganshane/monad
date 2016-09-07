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

  void TopBitSet::Debug() {
    TopBitSetIterator it = Iterator();
    printf("------- TopBitSet::Debug --- \n");
    while (it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS) {
      TopDoc *doc = it.Doc();
      printf("doc_id :%u freq:%u position:%lld \n", doc->doc, doc->freq, doc->position[0]);
    }
    printf("------- TopBitSet::Debug END  \n");
  }
  void RegionTopDoc::Debug(RegionTopDoc **docs, int32_t data_len) {
    for(int i=0;i<data_len;i++){
      RegionTopDoc* regionTopDoc = docs[i];
      printf("r:%d d:%d f:%d \n",regionTopDoc->region,regionTopDoc->top_doc->doc,regionTopDoc->top_doc->freq);
    }
  }
}//namespace monad
