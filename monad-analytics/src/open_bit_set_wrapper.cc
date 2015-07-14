// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#include "open_bit_set_wrapper.h"

#include <algorithm>
#include <vector>

#include "bit_set_region.h"
#include "bit_set_wrapper_holder.h"
#include "bit_set_wrapper_iterator.h"
#include "open_bit_set_iterator.h"
#include "open_bit_set_operator.h"
#include "top_bit_set_iterator.h"
#include "top_bit_set_wrapper.h"

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

  BitSetWrapperIterator<TopBitSetWrapper, TopBitSet>* TopBitSetWrapper::Iterator() {
    return new BitSetWrapperIterator<TopBitSetWrapper, TopBitSet>(this);
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

  OpenBitSetWrapper* OpenBitSetWrapper::InPlaceAnd(BitSetWrapperHolder<OpenBitSetWrapper>& holder) {
    OpenBitSetWrapper** wrappers = new OpenBitSetWrapper*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    OpenBitSetWrapper* result =InPlaceAnd(wrappers,holder.Size());
    delete[] wrappers;
    return result;
  }
  OpenBitSetWrapper* OpenBitSetWrapper::InPlaceAnd(OpenBitSetWrapper** wrappers,size_t coll_size) {
    if(coll_size == 0)
      return NULL;
    BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>** its = 
	    new BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>*[coll_size];
    uint32_t last_region = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    BitSetRegion<OpenBitSet>* tmp_region = NULL;

    //先计算出来最小区域
    for (uint32_t i = 0; i < coll_size; i++) {
      its[i] = wrappers[i]->Iterator();
      if (its[i]->NextRegion() != NULL) {
        tmp_region = its[i]->Region();
        if (last_region > tmp_region->region) {
          last_region = tmp_region->region;
        }
      }
    }
    //采取多路合并策略
    std::vector<OpenBitSet*> waiting_coll;
    OpenBitSetWrapper* wrapper = new OpenBitSetWrapper();
    while (true) {
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      waiting_coll.clear();

      for (uint32_t i = 0; i < coll_size; i++) {
        tmp_region = its[i]->Region();
        if (tmp_region == NULL)
          continue;
        if (last_region == tmp_region->region) {
          waiting_coll.push_back(tmp_region->bit_set);
          tmp_region = its[i]->NextRegion();
        }
        //计算下一个最小值
        if (tmp_region != NULL && next_min > tmp_region->region) {
          next_min = tmp_region->region;
        }
      }
      if (waiting_coll.size() == coll_size) {
        //printf("c:%u,w:%u ,last_region:%u,next_min:%u \n",coll_size,static_cast<uint32_t>(waiting_coll.size()),last_region,next_min);
        //进行合并操作
        OpenBitSet** arr = new OpenBitSet*[waiting_coll.size()];
        std::vector<OpenBitSet*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        OpenBitSet* result = OpenBitSetOperator::InPlaceAnd(arr, waiting_coll.size());
        BitSetRegion<OpenBitSet>* region = new BitSetRegion<OpenBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);

        delete[] arr;
      }

      if (next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_region = next_min;
    }

    //释放内存
    for (uint32_t i = 0; i < coll_size; i++) {
      delete its[i];
    }
    delete[] its;
    wrapper->Commit();
    return wrapper;
  }
  OpenBitSetWrapper* OpenBitSetWrapper::InPlaceOr(BitSetWrapperHolder<OpenBitSetWrapper>& holder) {
    OpenBitSetWrapper** wrappers = new OpenBitSetWrapper*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    OpenBitSetWrapper* result = InPlaceOr(wrappers,holder.Size());
    delete[] wrappers;
    return result;
  }
  OpenBitSetWrapper* OpenBitSetWrapper::InPlaceOr(OpenBitSetWrapper** wrappers,size_t coll_size) {
    if(coll_size == 0)
      return NULL;

    BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>** its =
      new BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>*[coll_size];
    uint32_t last_region = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    BitSetRegion<OpenBitSet>* tmp_region = NULL;

    //先计算出来最小区域
    for (uint32_t i = 0; i < coll_size; i++) {
      its[i] = wrappers[i]->Iterator();
      if (its[i]->NextRegion() != NULL) {
        tmp_region = its[i]->Region();
        if (last_region > tmp_region->region) {
          last_region = tmp_region->region;
        }
      }
    }
    //采取多路合并策略
    std::vector<OpenBitSet*> waiting_coll;
    OpenBitSetWrapper* wrapper = new OpenBitSetWrapper();
    while (true) {
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      waiting_coll.clear();

      for (uint32_t i = 0; i < coll_size; i++) {
        tmp_region = its[i]->Region();
        if (tmp_region == NULL)
          continue;
        if (last_region == tmp_region->region) {
          waiting_coll.push_back(tmp_region->bit_set);
          tmp_region = its[i]->NextRegion();
        }
        //计算下一个最小值
        if (tmp_region != NULL && next_min > tmp_region->region) {
          next_min = tmp_region->region;
        }
      }
      if (waiting_coll.size() > 0) {
        //printf("c:%u,w:%u ,last_region:%u,next_min:%u \n",coll_size,static_cast<uint32_t>(waiting_coll.size()),last_region,next_min);
        //进行合并操作
        OpenBitSet** arr = new OpenBitSet*[waiting_coll.size()];
        std::vector<OpenBitSet*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        OpenBitSet* result = OpenBitSetOperator::InPlaceOr(arr, waiting_coll.size());
        BitSetRegion<OpenBitSet>* region = new BitSetRegion<OpenBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_region = next_min;
    }

    //释放内存
    for (uint32_t i = 0; i < coll_size; i++) {
      delete its[i];
    }
    delete[] its;
    wrapper->Commit();
    return wrapper;
  }

  OpenBitSetWrapper* OpenBitSetWrapper::InPlaceNot(BitSetWrapperHolder<OpenBitSetWrapper>& holder) {
    OpenBitSetWrapper** wrappers = new OpenBitSetWrapper*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    OpenBitSetWrapper* result = InPlaceNot(wrappers,holder.Size());
    delete[] wrappers;
    return result;
  }
  OpenBitSetWrapper* OpenBitSetWrapper::InPlaceNot(OpenBitSetWrapper** wrappers,size_t coll_size) {
    if(coll_size == 0)
      return NULL;
    BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>** its = 
	    new BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>*[coll_size];
    uint32_t last_region = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    BitSetRegion<OpenBitSet>* tmp_region = NULL;

    //先计算出来最小区域
    for (uint32_t i = 0; i < coll_size; i++) {
      its[i] = wrappers[i]->Iterator();
      if (its[i]->NextRegion() != NULL) {
        tmp_region = its[i]->Region();
        if (last_region > tmp_region->region) {
          last_region = tmp_region->region;
        }
      }
    }
    //采取多路合并策略
    std::vector<OpenBitSet*> waiting_coll;
    OpenBitSetWrapper* wrapper = new OpenBitSetWrapper();
    bool first_hit = false; //记录第一条记录是否匹配
    while (true) {
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      waiting_coll.clear();
      first_hit = false;

      for (uint32_t i = 0; i < coll_size; i++) {
        tmp_region = its[i]->Region();
        if (tmp_region == NULL)
          continue;
        if (last_region == tmp_region->region) {
          waiting_coll.push_back(tmp_region->bit_set);
          tmp_region = its[i]->NextRegion();
          if (i == 0)
            first_hit = true;
        }
        //计算下一个最小值
        if (tmp_region != NULL && next_min > tmp_region->region) {
          next_min = tmp_region->region;
        }
      }
      if (waiting_coll.size() > 0 && first_hit) {
        //printf("c:%u,w:%u ,last_region:%u,next_min:%u \n",coll_size,static_cast<uint32_t>(waiting_coll.size()),last_region,next_min);
        //进行合并操作
        OpenBitSet** arr = new OpenBitSet*[waiting_coll.size()];
        std::vector<OpenBitSet*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        OpenBitSet* result = OpenBitSetOperator::InPlaceNot(arr, waiting_coll.size());
        BitSetRegion<OpenBitSet>* region = new BitSetRegion<OpenBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_region = next_min;
    }

    //释放内存
    for (uint32_t i = 0; i < coll_size; i++) {
      delete its[i];
    }
    delete[] its;
    wrapper->Commit();
    return wrapper;
  }

  TopBitSetWrapper* OpenBitSetWrapper::InPlaceAndTop(BitSetWrapperHolder<OpenBitSetWrapper>& holder, int32_t min_freq) {
    OpenBitSetWrapper** wrappers = new OpenBitSetWrapper*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    TopBitSetWrapper* result = InPlaceAndTop(wrappers,holder.Size(),min_freq);
    delete[] wrappers;
    return result;
  }
  TopBitSetWrapper* OpenBitSetWrapper::InPlaceAndTop(OpenBitSetWrapper** wrappers,size_t coll_size, int32_t min_freq) {
    BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>** its = 
	    new BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>*[coll_size];
    uint32_t last_region = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    BitSetRegion<OpenBitSet>* tmp_region = NULL;

    //先计算出来最小区域
    for (uint32_t i = 0; i < coll_size; i++) {
      its[i] = wrappers[i]->Iterator();
      if (its[i]->NextRegion() != NULL) {
        tmp_region = its[i]->Region();
        if (last_region > tmp_region->region) {
          last_region = tmp_region->region;
        }
      }
    }
    //采取多路合并策略
    std::vector<OpenBitSet*> waiting_coll;
    OpenBitSet empty_bit_set(1);
    TopBitSetWrapper* wrapper = new TopBitSetWrapper();
    while (true) {
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      waiting_coll.clear();

      for (uint32_t i = 0; i < coll_size; i++) {
        tmp_region = its[i]->Region();
        if (tmp_region == NULL){//加入为空，插入空集合便于占位处理位置信息
          waiting_coll.push_back(&empty_bit_set);
          continue;
        }
        if (last_region == tmp_region->region) {
          waiting_coll.push_back(tmp_region->bit_set);
          tmp_region = its[i]->NextRegion();
        }else{//如果没有此区域的数据，则写入空的集合
          waiting_coll.push_back(&empty_bit_set);
        }
        //计算下一个最小值
        if (tmp_region != NULL && next_min > tmp_region->region) {
          next_min = tmp_region->region;
        }
      }
      if (waiting_coll.size() > 0) {
        //printf("c:%lu,w:%u ,last_region:%u,next_min:%u \n",coll_size,static_cast<uint32_t>(waiting_coll.size()),last_region,next_min);
        //进行合并操作
        OpenBitSet** arr = new OpenBitSet*[waiting_coll.size()];
        std::vector<OpenBitSet*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        TopBitSet* result = OpenBitSetOperator::InPlaceAndTop(arr, waiting_coll.size(), min_freq);
        BitSetRegion<TopBitSet>* region = new BitSetRegion<TopBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_region = next_min;
    }

    //释放内存
    for (uint32_t i = 0; i < coll_size; i++) {
      delete its[i];
    }
    delete[] its;
    wrapper->Commit();
    return wrapper;
  }

  TopBitSetWrapper* OpenBitSetWrapper::InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq) {
      TopBitSetWrapper** wrappers = new TopBitSetWrapper*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    TopBitSetWrapper* result = InPlaceAndTopWithPositionMerged(wrappers,holder.Size(),min_freq);
    delete[] wrappers;
    return result;
  }
  TopBitSetWrapper* OpenBitSetWrapper::InPlaceAndTopWithPositionMerged
    (TopBitSetWrapper** wrappers,size_t coll_size, int32_t min_freq) {
    if(coll_size == 0)
      return NULL;
    BitSetWrapperIterator<TopBitSetWrapper, TopBitSet>** its = 
	    new BitSetWrapperIterator<TopBitSetWrapper, TopBitSet>*[coll_size];
    uint32_t last_region = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    BitSetRegion<TopBitSet>* tmp_region = NULL;

    //先计算出来最小区域
    for (uint32_t i = 0; i < coll_size; i++) {
      its[i] = wrappers[i]->Iterator();
      if (its[i]->NextRegion() != NULL) {
        tmp_region = its[i]->Region();
        if (last_region > tmp_region->region) {
          last_region = tmp_region->region;
        }
      }
    }
    //采取多路合并策略
    std::vector<TopBitSet*> waiting_coll;
    TopBitSetWrapper* wrapper = new TopBitSetWrapper();
    while (true) {
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      waiting_coll.clear();

      for (uint32_t i = 0; i < coll_size; i++) {
        tmp_region = its[i]->Region();
        if (tmp_region == NULL)
          continue;
        if (last_region == tmp_region->region) {
          waiting_coll.push_back(tmp_region->bit_set);
          tmp_region = its[i]->NextRegion();
        }
        //计算下一个最小值
        if (tmp_region != NULL && next_min > tmp_region->region) {
          next_min = tmp_region->region;
        }
      }
      if (waiting_coll.size() > 0) {
        //printf("c:%u,w:%u ,last_region:%u,next_min:%u \n",coll_size,static_cast<uint32_t>(waiting_coll.size()),last_region,next_min);
        //进行合并操作
        TopBitSet** arr = new TopBitSet*[waiting_coll.size()];
        std::vector<TopBitSet*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        TopBitSet* result = OpenBitSetOperator::InPlaceAndTopWithPositionMerged(arr, waiting_coll.size(), min_freq);
        BitSetRegion<TopBitSet>* region = new BitSetRegion<TopBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_region = next_min;
    }

    //释放内存
    for (uint32_t i = 0; i < coll_size; i++) {
      delete its[i];
    }
    delete[] its;
    wrapper->Commit();
    return wrapper;
  }
} //namespace monad
