// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_SPARSE_BIT_SET_WRAPPER_H_
#define MONAD_SPARSE_BIT_SET_WRAPPER_H_
#include <assert.h>
#include <vector>
#include "open_bit_set.h"
#include "top_bit_set.h"
#include "bit_set_region.h"
#include "sparse_bit_set.h"

namespace monad {
  template<typename T>
  struct BitSetRegion;

  template<typename WRAPPER, typename BIT_SET>
  class BitSetWrapperIterator;

  class TopBitSetWrapper;

  template<typename T>
  class BitSetWrapperHolder;

  class SparseBitSetWrapper {
  public:
    SparseBitSetWrapper();
    virtual ~SparseBitSetWrapper();
    void NewSeg(int32_t region, int32_t num_words);
    void ReadIndice(int32_t index,int64_t i){
      //_seg->bit_set->Read
    }
    void CreateBit(uint32_t index,uint32_t size){
      //_bits[index] = new Uint64Array(size);
    }
    void ReadBitBlock(uint32_t index,uint32_t block_index,uint64_t i){
      //_bits[index]->Set(block_index,i);
    }
    void ReadLong(int64_t data, int32_t index);
    void ReadLong(int8_t* long_byte_data, int32_t index);
    void ReadLong(int8_t* long_byte_data, int32_t from,int32_t to);
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
    static SparseBitSetWrapper* InPlaceAnd(SparseBitSetWrapper** wrappers,size_t len);
    static TopBitSetWrapper* InPlaceAndTop(SparseBitSetWrapper** wrappers, size_t len,int32_t min_freq);
    static TopBitSetWrapper* InPlaceAndTopWithPositionMerged(TopBitSetWrapper** wrappers, size_t len, int32_t min_freq);
    static SparseBitSetWrapper* InPlaceOr(SparseBitSetWrapper** wrappers, size_t len);
    static SparseBitSetWrapper* InPlaceNot(SparseBitSetWrapper** wrappers, size_t len);


    static SparseBitSetWrapper* InPlaceAnd(BitSetWrapperHolder<SparseBitSetWrapper>& holder);
    static TopBitSetWrapper* InPlaceAndTop(BitSetWrapperHolder<SparseBitSetWrapper>& holder, int32_t min_freq);
    static TopBitSetWrapper* InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq);
    static SparseBitSetWrapper* InPlaceOr(BitSetWrapperHolder<SparseBitSetWrapper>& holder);
    static SparseBitSetWrapper* InPlaceNot(BitSetWrapperHolder<SparseBitSetWrapper>& holder);
  private:
    uint32_t _weight;
    BitSetRegion<SparseBitSet>* _seg;
    std::vector<BitSetRegion<SparseBitSet>*> _data;
    BitSetWrapperIterator<SparseBitSetWrapper, SparseBitSet>* Iterator();
    friend class BitSetWrapperIterator<SparseBitSetWrapper, SparseBitSet>;
  };
}//namespace monad
#endif //MONAD_OPEN_BIT_SET_WRAPPER_H_
