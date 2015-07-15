// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_H_
#define MONAD_OPEN_BIT_SET_H_

#include <stdint.h>
#include <stdlib.h>

#include "config.h"
#include "bit_set.h"

namespace monad {

  /**
   * 记录一个集合的数据
   */
  class OpenBitSet :public BitSet<OpenBitSet>{
  public:
    /**
     *
     * @param num_words 该集合使用long的个数
     */
    OpenBitSet(uint32_t num_words);

    virtual ~OpenBitSet() {
      if (_bits) {
        free(_bits);
      }
    };
    /**
     * 读取一个long类型的数据
     * @param word 数据
     * @param index 写入的位置
     */
    virtual void ReadLong(uint64_t word, uint32_t index);
    /**
     * 读取Long类型的字节,该字节应该有8个字节
     * @param word 字节数据
     * @param index 写入的位置
     */
    virtual void ReadLong(int8_t* word, uint32_t index);
    virtual void ReadLong(int8_t* word, uint32_t from,uint32_t to);

    /**
     * 得到uint64_t类型的数据个数
     * @return 数据个数
     */
    virtual uint32_t GetNumWords() {
      return _num_words;
    };
    virtual uint32_t GetWordsLength() {
      return _words_len;
    };
    /**
     * 得到包含的数据
     * @return  数据
     */
    uint64_t* GetBits(){
      return _bits;
    }

    /**
     * 去掉尾部为0的数据
     */
    void TrimTrailingZeros() {
      int32_t idx = _words_len- 1;
      while (idx >= 0 && _bits[idx] == 0)
        --idx;
      _words_len= idx + 1;
    };
    bool Get(uint32_t index);
    bool FastGet(uint32_t index);
    bool Get(uint64_t index);
    bool FastGet(uint64_t index);
    void FastSet(uint32_t index);
    void Set(uint32_t index);
    void FastSet(uint64_t index);
    void EnsureCapacityWords(uint32_t num_words);
    void operator-=(const OpenBitSet& other);
    void operator+=(const OpenBitSet& other);
    void operator&=(const OpenBitSet& other);

    OpenBitSet* Clone();
    BitSetIterator* ToIterator();
    
    void Remove(const OpenBitSet& other) {
      this->operator -=(other);
    };

    void Union(const OpenBitSet& other) {
      this->operator +=(other);
    };
    void Or(const OpenBitSet& other) {
      this->operator +=(other);
    };

    void And(const OpenBitSet& other) {
      this->operator &=(other);
    };
    int32_t Weight(){
      return _weight;
    };
    void SetWeight(int32_t weight){
      _weight = weight;
    };
    uint64_t BitCount(){
      return PopArray(_bits,0,_num_words);
    };

  private:
    uint64_t* AllocateMemory(uint32_t num_words);
    static uint64_t PopArray(const uint64_t* A, uint32_t wordOffset, uint32_t numWords);
    uint32_t ExpandingWordNum(uint64_t index);
    uint64_t* _bits;
    uint32_t _num_words;
    uint32_t _words_len;
    int32_t _weight;
  };
}
#endif //ANALYTICS_OPEN_BIT_SET_H_
