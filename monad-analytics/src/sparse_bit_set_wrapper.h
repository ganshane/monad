// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_SPARSE_BIT_SET_WRAPPER_H_
#define MONAD_SPARSE_BIT_SET_WRAPPER_H_
#include <assert.h>

#include <vector>
#include "bit_set_wrapper.h"
#include "bit_set_region.h"
#include "sparse_bit_set.h"
#include "top_bit_set.h"

namespace monad {
  template<typename T>
  struct BitSetRegion;

  template<typename WRAPPER, typename BIT_SET>
  class BitSetWrapperIterator;

  class SparseBitSetWrapper;
  class TopBitSetWrapper;

  template<typename T>
  class BitSetWrapperHolder;

  class SparseBitSetWrapper:public BitSetWrapper<SparseBitSetWrapper,SparseBitSet> {
  public:
    SparseBitSetWrapper();
    virtual ~SparseBitSetWrapper();
    void NewSeg(int32_t region, int32_t num_words);
    void ReadIndice(int32_t index,int64_t i){
      _seg->bit_set->ReadIndice(index,i);
    }
    void CreateBit(int32_t index,int32_t size){
      _seg->bit_set->CreateBit(index,size);
    }
    void ReadBitBlock(int32_t index,int32_t block_index,int64_t i){
      _seg->bit_set->ReadBitBlock(index,block_index,i);
    }
    void FastSet(int32_t index);
    void Set(int32_t index);
    void TrimTrailingZeros();
    void Commit();
    bool FastGet(int32_t index);
    void SetWeight(int32_t weight);
    int32_t BitCount();
    /**
     * 读取集合中的前N个
     * @param result 传入的结果的对象
     * @param n 取前N个
     * @return 实际取到的个数
     */
    monad::RegionDoc** Top(int32_t n, int32_t& data_len);
  private:
    uint32_t _weight;
    BitSetRegion<SparseBitSet>* _seg;
    std::vector<BitSetRegion<SparseBitSet>*> _data;
    BitSetWrapperIterator<SparseBitSetWrapper, SparseBitSet>* Iterator();
    friend class BitSetWrapperIterator<SparseBitSetWrapper, SparseBitSet>;
    friend class BitSetWrapper<SparseBitSetWrapper, SparseBitSet>;
  };
}//namespace monad
#endif //MONAD_OPEN_BIT_SET_WRAPPER_H_
