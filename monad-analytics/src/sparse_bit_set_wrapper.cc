// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#include "sparse_bit_set_wrapper.h"

#include <algorithm>
#include "sparse_bit_set_iterator.h"

namespace monad {
  SparseBitSetWrapper::SparseBitSetWrapper():BitSetWrapper(){
  };
  BitSetWrapperIterator<SparseBitSetWrapper, SparseBitSet>* SparseBitSetWrapper::Iterator() {
    return new BitSetWrapperIterator<SparseBitSetWrapper, SparseBitSet>(this);
  };

  void SparseBitSetWrapper::NewSeg(int32_t region, int32_t num_words) {
    _seg = new BitSetRegion<SparseBitSet>();
    _seg->region = static_cast<uint32_t> (region);
    _seg->bit_set = new SparseBitSet(num_words);
    _seg->bit_set->SetWeight(_weight);

    _data.push_back(_seg);
  }

  void SparseBitSetWrapper::FastSet(int32_t index) {
    _seg->bit_set->Set(static_cast<uint32_t>(index));
  }
  void SparseBitSetWrapper::Set(int32_t index) {
    _seg->bit_set->Set(static_cast<uint32_t>(index));
  }
  void SparseBitSetWrapper::TrimTrailingZeros(){
    //_seg->bit_set->TrimTrailingZeros();
  }

  void SparseBitSetWrapper::Commit() {
    _seg = NULL;
    //按照数据分区的位置进行排序
    std::sort(_data.begin(), _data.end(), SortBitSetRegion<SparseBitSet>);
    /*
    std::vector<BitSetRegion<SparseBitSet>*>::iterator it = _data.begin();
    _total_doc = 0;
    for (; it != _data.end(); it++) {
      _total_doc += (*it)->bit_set->GetWordsLength() << 6; // *64
    }
    */
  }

  void SparseBitSetWrapper::SetWeight(int32_t weight) {
    _weight = static_cast<uint32_t> (weight);
    std::vector<BitSetRegion<SparseBitSet>*>::iterator it = _data.begin();
    for (; it != _data.end(); it++)
      (*it)->bit_set->SetWeight(_weight);
  }

  int32_t SparseBitSetWrapper::BitCount() {
    std::vector<BitSetRegion<SparseBitSet>*>::iterator it = _data.begin();
    int32_t r = 0;

    for (; it != _data.end(); it++)
      r += (*it)->bit_set->Cardinality();

    return r;
  }

  bool SparseBitSetWrapper::FastGet(int32_t index) {
    //printf("index:%u,total:%u \n",index,_total_doc);
    std::vector<BitSetRegion<SparseBitSet>*>::iterator it = _data.begin();
    uint32_t doc = 0;
    uint32_t last_doc_start = 0;
    for (; it != _data.end(); it++) {
      doc += ((*it)->bit_set->GetWordsLength()); // *64
      //printf("doc %u last_doc_start %u index %u\n",doc,last_doc_start,index);
      if (doc > static_cast<uint32_t> (index)) {
        return (*it)->bit_set->FastGet(index - last_doc_start);
      }
      last_doc_start = doc;
    }
    return false;
  }

  RegionDoc** SparseBitSetWrapper::Top(int32_t n, int32_t& data_len) {
    int32_t len = BitCount();
    data_len = (n < len) ? n : len;
    //printf("n:%d BitCount():%d data_len %d \n",n,len,data_len);
    RegionDoc** result = new RegionDoc*[data_len]();
    std::vector<BitSetRegion<SparseBitSet>*>::iterator it = _data.begin();
    uint32_t i = 0;
    for (; it != _data.end(); it++) {
      SparseBitSetIterator doc_it((*it)->bit_set);
      while (doc_it.NextDoc() != SparseBitSetIterator::NO_MORE_DOCS && i < static_cast<uint32_t> (data_len)) {
        RegionDoc* doc = new RegionDoc();
        doc->doc = doc_it.DocId();
        doc->region = (*it)->region;
        result[i++] = doc;
      }
    }
    return result;
  }
} //namespace monad
