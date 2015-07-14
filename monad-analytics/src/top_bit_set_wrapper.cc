// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#include "top_bit_set_wrapper.h"

#include <algorithm>

#include "bit_set_wrapper_iterator.h"
#include "priority_queue.h"
#include "top_bit_set.h"
#include "top_bit_set_iterator.h"

namespace monad{
  bool SortRegionTopDocByFreq(RegionTopDoc* a, RegionTopDoc* b) {
    return a->top_doc->freq <= b->top_doc->freq;
  };
  TopBitSetWrapper::~TopBitSetWrapper() {
    std::vector<BitSetRegion<TopBitSet>*>::iterator it = _data.begin();
    for (; it != _data.end(); it++) {
      delete *it;
    }
  }
  int32_t TopBitSetWrapper::BitCount(){
    return _total_doc;
  }
  void TopBitSetWrapper::Commit() {
    std::sort(_data.begin(), _data.end(), SortBitSetRegion<TopBitSet>);
    std::vector<BitSetRegion<TopBitSet>*>::iterator it = _data.begin();
    _total_doc = 0;
    for (; it != _data.end(); it++) {
      _total_doc += (*it)->bit_set->DocCount();
    }
  }
  RegionTopDoc** TopBitSetWrapper::Top(int32_t n, int32_t& data_len) {
    data_len = (static_cast<uint32_t>(n) < _total_doc) ? n : _total_doc;
    PriorityQueue<RegionTopDoc> pq(data_len,SortRegionTopDocByFreq);
    std::vector<BitSetRegion<TopBitSet>*>::iterator it = _data.begin();
    RegionTopDoc* doc = NULL;
    for (; it != _data.end(); it++) {
      TopBitSetIterator doc_it((*it)->bit_set);
      while (doc_it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS) {
        if(!doc)
          doc = new RegionTopDoc();
        doc->top_doc = doc_it.Doc();
        doc->region = (*it)->region;

        doc = pq.InsertWithOverflow(doc);
      }
    }
    if(doc)
      delete doc;
    RegionTopDoc** result = new RegionTopDoc*[data_len]();
    for(int i= data_len - 1;i > -1;i--){
      result[i] = pq.Pop();
    }
    return result;
  }
} //namespace monad
