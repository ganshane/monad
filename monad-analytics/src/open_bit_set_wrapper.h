// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_WRAPPER_H_
#define MONAD_OPEN_BIT_SET_WRAPPER_H_
#include <assert.h>
#include <vector>

#include "bit_set_wrapper.h"
#include "open_bit_set.h"
#include "top_bit_set.h"

namespace monad {
  template<typename T>
  struct BitSetRegion;

  template<typename WRAPPER, typename BIT_SET>
  class BitSetWrapperIterator;

  class TopBitSetWrapper;

  template<typename T>
  class BitSetWrapperHolder;

  class OpenBitSetWrapper :public BitSetWrapper<OpenBitSetWrapper,OpenBitSet>{
  public:
    OpenBitSetWrapper();
    virtual ~OpenBitSetWrapper();
    void NewSeg(int32_t region, int32_t num_words);
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
    /*
    static OpenBitSetWrapper* InPlaceAnd(OpenBitSetWrapper** wrappers,size_t len){
      return BitSetWrapper<OpenBitSetWrapper,OpenBitSet>::InPlaceAnd(wrappers,len);
    }
    static TopBitSetWrapper* InPlaceAndTop(OpenBitSetWrapper** wrappers, size_t len,int32_t min_freq);
    static TopBitSetWrapper* InPlaceAndTopWithPositionMerged(TopBitSetWrapper** wrappers, size_t len, int32_t min_freq);
    static OpenBitSetWrapper* InPlaceOr(OpenBitSetWrapper** wrappers, size_t len);
    static OpenBitSetWrapper* InPlaceNot(OpenBitSetWrapper** wrappers, size_t len);


    static OpenBitSetWrapper* InPlaceAnd(BitSetWrapperHolder<OpenBitSetWrapper>& holder){
      return BitSetWrapper<OpenBitSetWrapper,OpenBitSet>::InPlaceAnd(holder);
    }
    static TopBitSetWrapper* InPlaceAndTop(BitSetWrapperHolder<OpenBitSetWrapper>& holder, int32_t min_freq);
    static TopBitSetWrapper* InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq);
    static OpenBitSetWrapper* InPlaceOr(BitSetWrapperHolder<OpenBitSetWrapper>& holder);
    static OpenBitSetWrapper* InPlaceNot(BitSetWrapperHolder<OpenBitSetWrapper>& holder);
     */
  private:
    uint32_t _weight;
    BitSetRegion<OpenBitSet>* _seg;
    std::vector<BitSetRegion<OpenBitSet>*> _data;
    BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>* Iterator();
    friend class BitSetWrapperIterator<OpenBitSetWrapper, OpenBitSet>;
    friend class BitSetWrapper<OpenBitSetWrapper,OpenBitSet>;
  };
}//namespace monad
#endif //MONAD_OPEN_BIT_SET_WRAPPER_H_
