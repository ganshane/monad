// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "roaring_bit_set_wrapper.h"

#include <algorithm>
#include "roaring_bit_set_iterator.h"

namespace monad {
  RoaringBitSetWrapper::RoaringBitSetWrapper():BitSetWrapper(){
  };
  BitSetWrapperIterator<RoaringBitSetWrapper, RoaringBitSet>* RoaringBitSetWrapper::Iterator() {
    return new BitSetWrapperIterator<RoaringBitSetWrapper, RoaringBitSet>(this);
  };

  RoaringBitSetWrapper* RoaringBitSetWrapper::FromTopBitSetWrapper(TopBitSetWrapper *wrapper) {
    RoaringBitSetWrapper* rbs_wrapper = new RoaringBitSetWrapper();
    BitSetWrapperIterator<TopBitSetWrapper,TopBitSet>* it = wrapper->Iterator();
    BitSetRegion<TopBitSet>* region = it->NextRegion();
    while(region){
      rbs_wrapper->NewSeg(region->region,1);
      TopBitSetIterator tbst(region->bit_set);
      while(tbst.NextDoc() != BitSetIterator::NO_MORE_DOCS){
        rbs_wrapper->FastSet(tbst.DocId());
      }
      region = it->NextRegion();
    }
    delete it;
    return rbs_wrapper;
  }

  uint32_t RoaringBitSetWrapper::NewSeg(int32_t region,const char* bb) {
    roaring_bitmap_t* underlying = roaring_bitmap_portable_deserialize(bb);
    if(underlying) {

      _seg = new BitSetRegion<RoaringBitSet>();
      _seg->region = static_cast<uint32_t> (region);
      _seg->bit_set = new RoaringBitSet();
      _seg->bit_set->_underlying = underlying;
      _seg->bit_set->SetWeight(_weight);

      _data.push_back(_seg);
      return roaring_bitmap_portable_size_in_bytes(underlying);
    }else{
      return 0;
    }
  }
  void RoaringBitSetWrapper::NewSeg(int32_t region, int32_t num_words) {
    _seg = new BitSetRegion<RoaringBitSet>();
    _seg->region = static_cast<uint32_t> (region);
    _seg->bit_set = new RoaringBitSet(num_words);
    _seg->bit_set->SetWeight(_weight);

    _data.push_back(_seg);
  }

  void RoaringBitSetWrapper::FastSet(int32_t index) {
    _seg->bit_set->Set(static_cast<uint32_t>(index));
  }
  void RoaringBitSetWrapper::Set(int32_t index) {
    _seg->bit_set->Set(static_cast<uint32_t>(index));
  }
  void RoaringBitSetWrapper::Optimize(){
    _seg->bit_set->Optimize();
  }

  void RoaringBitSetWrapper::Commit() {
    _seg = NULL;
    //按照数据分区的位置进行排序
    std::sort(_data.begin(), _data.end(), SortBitSetRegion<RoaringBitSet>);
    /*
    std::vector<BitSetRegion<RoaringBitSet>*>::iterator it = _data.begin();
    _total_doc = 0;
    for (; it != _data.end(); it++) {
      _total_doc += (*it)->bit_set->GetWordsLength() << 6; // *64
    }
    */
  }

  void RoaringBitSetWrapper::SetWeight(int32_t weight) {
    _weight = static_cast<uint32_t> (weight);
    std::vector<BitSetRegion<RoaringBitSet>*>::iterator it = _data.begin();
    for (; it != _data.end(); it++)
      (*it)->bit_set->SetWeight(_weight);
  }

  uint32_t RoaringBitSetWrapper::SegCount(){
    return _data.size();
  }
  int32_t RoaringBitSetWrapper::BitCount() {
    std::vector<BitSetRegion<RoaringBitSet>*>::iterator it = _data.begin();
    int32_t r = 0;

    for (; it != _data.end(); it++)
      r += (*it)->bit_set->Cardinality();

    return r;
  }

  bool RoaringBitSetWrapper::FastGet(int32_t index) {
    //printf("index:%u,total:%u \n",index,_total_doc);
    std::vector<BitSetRegion<RoaringBitSet>*>::iterator it = _data.begin();
    uint32_t doc = 0;
    uint32_t last_doc_start = 0;
    for (; it != _data.end(); it++) {
      doc += ((*it)->bit_set->GetWordsLength()); // *64
//      printf("doc %u last_doc_start %u index %u\n",doc,last_doc_start,index);
      if (doc > static_cast<uint32_t> (index)) {
        return (*it)->bit_set->Get(index - last_doc_start);
      }
      last_doc_start = doc;
    }
    return false;
  }

  RegionDoc** RoaringBitSetWrapper::Top(int32_t n, int32_t& data_len) {
    int32_t len = BitCount();
    data_len = (n < len) ? n : len;
    //printf("n:%d BitCount():%d data_len %d \n",n,len,data_len);
    RegionDoc** result = new RegionDoc*[data_len]();
    std::vector<BitSetRegion<RoaringBitSet>*>::iterator it = _data.begin();
    uint32_t i = 0;
    for (; it != _data.end(); it++) {
      RoaringBitSetIterator doc_it((*it)->bit_set);
      while (doc_it.NextDoc() != RoaringBitSetIterator::NO_MORE_DOCS && i < static_cast<uint32_t> (data_len)) {
        RegionDoc* doc = new RegionDoc();
        doc->doc = doc_it.DocId();
        doc->region = (*it)->region;
        result[i++] = doc;
      }
    }
    return result;
  }
} //namespace monad
