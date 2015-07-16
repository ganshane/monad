// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#include "open_bit_set_wrapper.h"

namespace monad {

  OpenBitSetWrapper::~OpenBitSetWrapper() {
    std::vector<BitSetRegion<OpenBitSet>*>::iterator it = _data.begin();
    for (; it != _data.end(); it++) {
      delete *it;
    }
  }

  OpenBitSetWrapper::OpenBitSetWrapper() 
  : _weight(1), _seg(NULL) {
  };

  BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>* OpenBitSetWrapper::Iterator() {
    return new BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>(this);
  };



  void OpenBitSetWrapper::NewSeg(int32_t region, int32_t num_words) {
    _seg = new BitSetRegion<OpenBitSet>();
    _seg->region = static_cast<uint32_t> (region);
    _seg->bit_set = new OpenBitSet(num_words);
    _seg->bit_set->SetWeight(_weight);

    _data.push_back(_seg);
  }

  void OpenBitSetWrapper::ReadLong(int64_t word, int32_t index) {
    _seg->bit_set->ReadLong(word, static_cast<uint32_t> (index));

  }

  void OpenBitSetWrapper::ReadLong(int8_t* word, int32_t index) {
    _seg->bit_set->ReadLong(word, static_cast<uint32_t> (index));
  }
  void OpenBitSetWrapper::ReadLong(int8_t* word, int32_t from,int32_t to) {
    _seg->bit_set->ReadLong(word, static_cast<uint32_t> (from),static_cast<uint32_t>(to));
  }
  void OpenBitSetWrapper::FastSet(int32_t index) {
    _seg->bit_set->FastSet(static_cast<uint32_t>(index));
  }
  void OpenBitSetWrapper::Set(int32_t index) {
    _seg->bit_set->Set(static_cast<uint32_t>(index));
  }
  void OpenBitSetWrapper::TrimTrailingZeros(){
    _seg->bit_set->TrimTrailingZeros();
  }

  void OpenBitSetWrapper::Commit() {
    _seg = NULL;
    //按照数据分区的位置进行排序
    std::sort(_data.begin(), _data.end(), SortBitSetRegion<OpenBitSet>);
    /*
    std::vector<BitSetRegion<OpenBitSet>*>::iterator it = _data.begin();
    _total_doc = 0;
    for (; it != _data.end(); it++) {
      _total_doc += (*it)->bit_set->GetWordsLength() << 6; // *64
    }
    */
  }

  void OpenBitSetWrapper::SetWeight(int32_t weight) {
    _weight = static_cast<uint32_t> (weight);
    std::vector<BitSetRegion<OpenBitSet>*>::iterator it = _data.begin();
    for (; it != _data.end(); it++)
      (*it)->bit_set->SetWeight(_weight);
  }

  int32_t OpenBitSetWrapper::BitCount() {
    std::vector<BitSetRegion<OpenBitSet>*>::iterator it = _data.begin();
    int32_t r = 0;

    for (; it != _data.end(); it++)
      r += (*it)->bit_set->BitCount();

    return r;
  }

  bool OpenBitSetWrapper::FastGet(int32_t index) {
    //printf("index:%u,total:%u \n",index,_total_doc);
    std::vector<BitSetRegion<OpenBitSet>*>::iterator it = _data.begin();
    uint32_t doc = 0;
    uint32_t last_doc_start = 0;
    for (; it != _data.end(); it++) {
      doc += ((*it)->bit_set->GetWordsLength() << 6); // *64
      //printf("doc %u last_doc_start %u index %u\n",doc,last_doc_start,index);
      if (doc > static_cast<uint32_t> (index)) {
        return (*it)->bit_set->FastGet(index - last_doc_start);
      }
      last_doc_start = doc;
    }
    return false;
  }

  RegionDoc** OpenBitSetWrapper::Top(int32_t n, int32_t& data_len) {
    int32_t len = BitCount();
    data_len = (n < len) ? n : len;
    //printf("n:%d bitCount():%d data_len %d \n",n,len,data_len);
    RegionDoc** result = new RegionDoc*[data_len]();
    std::vector<BitSetRegion<OpenBitSet>*>::iterator it = _data.begin();
    uint32_t i = 0;
    for (; it != _data.end(); it++) {
      OpenBitSetIterator doc_it(*(*it)->bit_set);
      while (doc_it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS && i < static_cast<uint32_t> (data_len)) {
        RegionDoc* doc = new RegionDoc();
        doc->doc = doc_it.DocId();
        doc->region = (*it)->region;
        result[i++] = doc;
      }
    }
    return result;
  }

} //namespace monad
