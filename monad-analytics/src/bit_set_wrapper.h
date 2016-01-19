// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_BIT_SET_WRAPPER_H_
#define MONAD_BIT_SET_WRAPPER_H_

#include "bit_set_operator.h"
#include "bit_set_wrapper_holder.h"
#include "bit_set_wrapper_iterator.h"
#include "top_bit_set_wrapper.h"

namespace monad{
  template<typename WRAPPER,typename BIT_SET>
  class BitSetWrapper {

  public:
    BitSetWrapper():_weight(1),_seg(NULL){}
    virtual ~BitSetWrapper() {
      typename std::vector<BitSetRegion<BIT_SET>*>::iterator it = _data.begin();
      for (; it != _data.end(); it++) {
        delete *it;
      }
    }
    static WRAPPER* InPlaceAnd(WRAPPER** wrappers,size_t len);
    static TopBitSetWrapper* InPlaceAndTop(WRAPPER** wrappers, size_t len,int32_t min_freq);
    static TopBitSetWrapper* InPlaceAndTopWithPositionMerged(TopBitSetWrapper** wrappers, size_t len, int32_t min_freq);
    static WRAPPER* InPlaceOr(WRAPPER** wrappers, size_t len);
    static WRAPPER* InPlaceNot(WRAPPER** wrappers, size_t len);


    static WRAPPER* InPlaceAnd(BitSetWrapperHolder<WRAPPER>& holder);
    static TopBitSetWrapper* InPlaceAndTop(BitSetWrapperHolder<WRAPPER>& holder, int32_t min_freq);
    static TopBitSetWrapper* InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq);
    static WRAPPER* InPlaceOr(BitSetWrapperHolder<WRAPPER>& holder);
    static WRAPPER* InPlaceNot(BitSetWrapperHolder<WRAPPER>& holder);

  protected:
    uint32_t _weight;
    BitSetRegion<BIT_SET>* _seg;
    std::vector<BitSetRegion<BIT_SET>*> _data;
    virtual BitSetWrapperIterator<WRAPPER, BIT_SET>* Iterator() = 0;
    friend class BitSetWrapperIterator<WRAPPER, BIT_SET>;
    //friend class BitSetWrapper<WRAPPER, BIT_SET>;
    //typedef BitSetWrapperIterator<WRAPPER, BIT_SET> bsi;
  };
  /*
  template<typename WRAPPER,typename BIT_SET>
  BitSetWrapperIterator<WRAPPER, BIT_SET>* BitSetWrapper<WRAPPER,BIT_SET>::Iterator(){
    return new BitSetWrapperIterator<WRAPPER,BIT_SET>(this);
  };
   */
  template<typename WRAPPER,typename BIT_SET>
  WRAPPER* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceAnd(BitSetWrapperHolder<WRAPPER>& holder) {
    WRAPPER** wrappers = new WRAPPER*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    WRAPPER* result =InPlaceAnd(wrappers,holder.Size());
    delete[] wrappers;
    return result;
  }
  template<typename WRAPPER,typename BIT_SET>
  WRAPPER* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceAnd(WRAPPER** wrappers,size_t coll_size) {
    if(coll_size == 0)
      return NULL;
    BitSetWrapperIterator<WRAPPER, BIT_SET>** its =
        new BitSetWrapperIterator<WRAPPER, BIT_SET>*[coll_size];
    uint32_t last_region = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    BitSetRegion<BIT_SET>* tmp_region = NULL;

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
    std::vector<BIT_SET*> waiting_coll;
    WRAPPER* wrapper = new WRAPPER();
    while (true) {
      next_min = BitSetIterator::NO_MORE_DOCS;
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
        BIT_SET** arr = new BIT_SET*[waiting_coll.size()];
        typename std::vector<BIT_SET*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        BIT_SET* result = BitSetOperator<BIT_SET>::InPlaceAnd(arr, waiting_coll.size());
        BitSetRegion<BIT_SET>* region = new BitSetRegion<BIT_SET>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);

        delete[] arr;
      }

      if (next_min == BitSetIterator::NO_MORE_DOCS)
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
  template<typename WRAPPER,typename BIT_SET>
  WRAPPER* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceOr(BitSetWrapperHolder<WRAPPER>& holder) {
    WRAPPER** wrappers = new WRAPPER*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    WRAPPER* result = InPlaceOr(wrappers,holder.Size());
    delete[] wrappers;
    return result;
  }
  template<typename WRAPPER,typename BIT_SET>
  WRAPPER* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceOr(WRAPPER** wrappers,size_t coll_size) {
    if(coll_size == 0)
      return NULL;

    BitSetWrapperIterator<WRAPPER, BIT_SET>** its =
        new BitSetWrapperIterator<WRAPPER, BIT_SET>*[coll_size];
    uint32_t last_region = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    BitSetRegion<BIT_SET>* tmp_region = NULL;

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
    std::vector<BIT_SET*> waiting_coll;
    WRAPPER* wrapper = new WRAPPER();
    while (true) {
      next_min = BitSetIterator::NO_MORE_DOCS;
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
        BIT_SET** arr = new BIT_SET*[waiting_coll.size()];
        typename std::vector<BIT_SET*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        BIT_SET* result = BitSetOperator<BIT_SET>::InPlaceOr(arr, waiting_coll.size());
        BitSetRegion<BIT_SET>* region = new BitSetRegion<BIT_SET>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == BitSetIterator::NO_MORE_DOCS)
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
  template<typename WRAPPER,typename BIT_SET>
  WRAPPER* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceNot(BitSetWrapperHolder<WRAPPER>& holder) {
    WRAPPER** wrappers = new WRAPPER*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    WRAPPER* result = InPlaceNot(wrappers,holder.Size());
    delete[] wrappers;
    return result;
  }
  template<typename WRAPPER,typename BIT_SET>
  WRAPPER* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceNot(WRAPPER** wrappers,size_t coll_size) {
    if(coll_size == 0)
      return NULL;
    BitSetWrapperIterator<WRAPPER, BIT_SET>** its =
        new BitSetWrapperIterator<WRAPPER, BIT_SET>*[coll_size];
    uint32_t last_region = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    BitSetRegion<BIT_SET>* tmp_region = NULL;

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
    std::vector<BIT_SET*> waiting_coll;
    WRAPPER* wrapper = new WRAPPER();
    bool first_hit = false; //记录第一条记录是否匹配
    while (true) {
      next_min = BitSetIterator::NO_MORE_DOCS;
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
        BIT_SET** arr = new BIT_SET*[waiting_coll.size()];
        typename std::vector<BIT_SET*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        BIT_SET* result = BitSetOperator<BIT_SET>::InPlaceNot(arr, waiting_coll.size());
        BitSetRegion<BIT_SET>* region = new BitSetRegion<BIT_SET>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == BitSetIterator::NO_MORE_DOCS)
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

  template<typename WRAPPER,typename BIT_SET>
  TopBitSetWrapper* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceAndTop(BitSetWrapperHolder<WRAPPER>& holder, int32_t min_freq) {
    WRAPPER** wrappers = new WRAPPER*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    TopBitSetWrapper* result = InPlaceAndTop(wrappers,holder.Size(),min_freq);
    delete[] wrappers;
    return result;
  }
  template<typename WRAPPER,typename BIT_SET>
  TopBitSetWrapper* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceAndTop(WRAPPER** wrappers,size_t coll_size, int32_t min_freq) {
    BitSetWrapperIterator<WRAPPER, BIT_SET>** its =
        new BitSetWrapperIterator<WRAPPER, BIT_SET>*[coll_size];
    uint32_t last_region = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    BitSetRegion<BIT_SET>* tmp_region = NULL;

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
    std::vector<BIT_SET*> waiting_coll;
    BIT_SET empty_bit_set(1);
    TopBitSetWrapper* wrapper = new TopBitSetWrapper();
    while (true) {
      next_min = BitSetIterator::NO_MORE_DOCS;
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
        BIT_SET** arr = new BIT_SET*[waiting_coll.size()];
        typename std::vector<BIT_SET*>::iterator waiting_it = waiting_coll.begin();
        for (int i = 0; waiting_it != waiting_coll.end(); waiting_it++, i++) {
          arr[i] = *waiting_it;
        }

        TopBitSet* result = BitSetOperator<BIT_SET>::InPlaceAndTop(arr, waiting_coll.size(), min_freq);
        BitSetRegion<TopBitSet>* region = new BitSetRegion<TopBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == BitSetIterator::NO_MORE_DOCS)
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

  template<typename WRAPPER,typename BIT_SET>
  TopBitSetWrapper* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq) {
    TopBitSetWrapper** wrappers = new TopBitSetWrapper*[holder.Size()];
    for(int32_t i=0;i<holder.Size();i++)
      wrappers[i] = holder.Get(i);
    TopBitSetWrapper* result = InPlaceAndTopWithPositionMerged(wrappers,holder.Size(),min_freq);
    delete[] wrappers;
    return result;
  }
  template<typename WRAPPER,typename BIT_SET>
  TopBitSetWrapper* BitSetWrapper<WRAPPER,BIT_SET>::InPlaceAndTopWithPositionMerged(TopBitSetWrapper** wrappers,size_t coll_size, int32_t min_freq) {
    if(coll_size == 0)
      return NULL;
    typedef BitSetWrapperIterator<TopBitSetWrapper,TopBitSet> BSWI;
    BSWI** its = new BSWI*[coll_size];
    uint32_t last_region = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    BitSetRegion<TopBitSet>* tmp_region = NULL;

//先计算出来最小区域
    for (uint32_t i = 0; i < coll_size; i++) {
      BitSetWrapperIterator<TopBitSetWrapper,TopBitSet>* it = wrappers[i]->Iterator();
      its[i] = it;
      if (it->NextRegion() != NULL) {
        tmp_region = it->Region();
        if (last_region > tmp_region->region) {
          last_region = tmp_region->region;
        }
      }
    }
//采取多路合并策略
    std::vector<TopBitSet*> waiting_coll;
    TopBitSetWrapper* wrapper = new TopBitSetWrapper();
    while (true) {
      next_min = BitSetIterator::NO_MORE_DOCS;
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

        TopBitSet* result = BitSetOperator<BIT_SET>::InPlaceAndTopWithPositionMerged(arr, waiting_coll.size(), min_freq);
        BitSetRegion<TopBitSet>* region = new BitSetRegion<TopBitSet>();
        region->region = last_region;
        region->bit_set = result;
        wrapper->_data.push_back(region);
        delete[] arr;
      }

      if (next_min == BitSetIterator::NO_MORE_DOCS)
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
}

#endif //MONAD_BIT_SET_WRAPPER_H_
