// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#include "open_bit_set.h"
#include "open_bit_set_iterator.h"

#include <algorithm>
#include <assert.h>
#include <stdio.h>
#include <string>
#include <string.h>

namespace monad {
  //采用big endian的模式来转换
  static inline uint32_t ConvertBytesToInt32(const int8_t* data,uint32_t offset=0){
    const int8_t* ptr= data+offset;
    return ((static_cast<uint32_t>(static_cast<unsigned char>(ptr[0])) << 24)
        | (static_cast<uint32_t>(static_cast<unsigned char>(ptr[1])) << 16)
        | (static_cast<uint32_t>(static_cast<unsigned char>(ptr[2])) << 8)
        | (static_cast<uint32_t>(static_cast<unsigned char>(ptr[3]))));
  }
  //采用big endian的模式来转换
  static inline uint64_t ConvertBytesToInt64(const int8_t* data,uint32_t offset=0){
    //前4位为高端、后四位为低端数据
    uint64_t hi = ConvertBytesToInt32(data,offset);
    uint64_t lo = ConvertBytesToInt32(data,4+offset);
    return (hi << 32) | lo;
  }
  

  OpenBitSet::OpenBitSet(uint32_t num_words) {
    this->_bits = AllocateMemory(num_words);
    this->_num_words = num_words;
    this->_weight = 1;
    this->_words_len = _num_words;//设置
  }
  OpenBitSet::~OpenBitSet() {
    if (_bits) {
      free(_bits);
    }
  };

  uint64_t* OpenBitSet::AllocateMemory(uint32_t num_words) {
    assert(num_words > 0);
    //生成内存空间
    uint32_t byte_len = num_words * sizeof (uint64_t);
    void* mem = malloc(byte_len);
    memset(mem, 0, byte_len);
    return static_cast<uint64_t*> (mem);
  }

  OpenBitSet* OpenBitSet::Clone() {
    OpenBitSet* bit_set = new OpenBitSet(_words_len);
    memcpy(bit_set->_bits, _bits, _words_len* sizeof (uint64_t));
    return bit_set;
  };

  BitSetIterator* OpenBitSet::ToIterator() {
    return new OpenBitSetIterator(*this);
  }

  void OpenBitSet::ReadLong(int8_t* word, uint32_t index) {
    this->ReadLong(ConvertBytesToInt64(word),index);
  }
  void OpenBitSet::ReadLong(int8_t* word, uint32_t from,uint32_t to) {
    for(uint32_t index = from;index <= to;index++){
      ReadLong(word+ ((index-from) * 8),index);
    }
  }
  void OpenBitSet::ReadLong(uint64_t word, uint32_t index) {
    assert(index < _num_words);
    //printf("read long [%u]=%llu \n",index,word);
    this->_bits[index] = word;
  }

  bool OpenBitSet::Get(uint32_t index) {
    uint32_t i = index >> 6; // div 64
    if (i >= _num_words)
      return false;
    uint32_t bit = (index & 0x3f); // mod 64
    uint64_t bitmask = 1LL << bit;
    return ((_bits[i] & bitmask) != 0);
  }

  bool OpenBitSet::FastGet(uint32_t index) {
    uint32_t i = index >> 6; // div 64
    uint32_t bit = (index & 0x3f); // mod 64
    uint64_t bitmask = 1LL << bit;
    return ((_bits[i] & bitmask) != 0);
  }

  bool OpenBitSet::Get(uint64_t index) {
    uint32_t i = (uint32_t) (index >> 6); // div 64
    if (i >= _num_words)
      return false;
    uint32_t bit = ((uint32_t) index & 0x3f); // mod 64
    uint64_t bitmask = 1LL << bit;
    return ((_bits[i] & bitmask) != 0);
  }

  bool OpenBitSet::FastGet(uint64_t index) {
    uint32_t i = (uint32_t) (index >> 6); // div 64
    uint32_t bit = ((uint32_t) index & 0x3f); // mod 64
    uint64_t bitmask = 1LL << bit;
    return ((_bits[i] & bitmask) != 0);
  }

  void OpenBitSet::FastSet(uint32_t index) {
    uint32_t wordNum = index >> 6; // div 64
    uint32_t bit = index & 0x3f;
    uint64_t bitmask = 1LL << bit;
    _bits[wordNum] |= bitmask;
  }

  void OpenBitSet::Set(uint32_t index) {
    uint32_t wordNum = ExpandingWordNum(index);
    uint32_t bit = (uint32_t) index & 0x3f;
    uint64_t bitmask = 1LL << bit;
    _bits[wordNum] |= bitmask;
  }
  uint32_t OpenBitSet::ExpandingWordNum(uint64_t index) {
    uint32_t num_words = static_cast<uint32_t>(index >> 6);
    if (num_words >= _num_words){
      EnsureCapacityWords(num_words+10);
      _words_len = num_words + 1;
    }
    return num_words;
  }

  void OpenBitSet::EnsureCapacityWords(uint32_t num_words) {
    if (_num_words < num_words) {
      /*
      void* new_ptr = malloc(num_words * sizeof(uint64_t));
      memset(new_ptr,0,num_words* sizeof(uint64_t));
      memcpy(new_ptr, _bits, _num_words * sizeof(uint64_t));
      free(_bits);
      _bits = (uint64_t *) new_ptr;
      _num_words = num_words;
       */
      void* new_ptr = realloc(_bits, num_words * sizeof (uint64_t));
      if (new_ptr) {
        _bits = static_cast<uint64_t*> (new_ptr);
        //设置扩展的内存空间为0
        memset(_bits+_num_words, 0, (num_words - _num_words)*sizeof(uint64_t));
        _num_words = num_words;
      }else{
        free(_bits);
        printf("realoc error!\n");
      }
    }
  }

  void OpenBitSet::operator-=(const OpenBitSet& other) {
    int32_t idx = std::min(_words_len, other._words_len);
    uint64_t* other_bits = other._bits;
    while (--idx >= 0)
      _bits[idx] &= ~other_bits[idx];
  }

  void OpenBitSet::operator+=(const OpenBitSet& other) {
    uint32_t new_len = std::max(_words_len, other._words_len);
    uint32_t old_num  = _num_words;
    EnsureCapacityWords(new_len);
    uint64_t* other_bits = other._bits;
    int32_t pos = std::min(_words_len, other._words_len);
    while (--pos >= 0)
      _bits[pos] |= other_bits[pos];
    if (old_num < new_len) {
      uint32_t offset = old_num* sizeof (uint64_t);
      memcpy(_bits + offset,
              other_bits + offset,
              (new_len - old_num) * sizeof (uint64_t));
    }
  }

  void OpenBitSet::operator&=(const OpenBitSet& other) {
    int32_t new_len = std::min(this->_words_len, other._words_len);
    uint64_t* other_bits = other._bits;
    int32_t pos = new_len;
    while (--pos >= 0)
      _bits[pos] &= other_bits[pos];
    if (this->_words_len > static_cast<uint32_t>(new_len)) {
      //设置新位置开始的内存空间为0
      memset(_bits+(new_len*sizeof(uint64_t)), 0, (_words_len -new_len)*sizeof(uint64_t) );
      /*
      for (uint32_t i = new_len; i < _words_len; i++)
        _bits[i] = 0L;
      */
    }
    _words_len = new_len;
  }
  static uint32_t pop(uint64_t x)
    {
        x = x - ((x >> 1ULL) & 0x5555555555555555ULL);
        x = (x & 0x3333333333333333ULL) + ((x >>2ULL) & 0x3333333333333333ULL);
        x = (x + (x >> 4ULL)) & 0x0f0f0f0f0f0f0f0fULL;
        x = x + (x >> 8ULL);
        x = x + (x >> 16ULL);
        x = x + (x >> 32ULL);
        return (uint32_t)x & 0x7f;
    }
  static void CSA(int64_t& h, int64_t& l, int64_t a, int64_t b, int64_t c)
  {
    int64_t u = a ^ b;
    h = (a & b) | (u & c);
    l = u ^ c;
  }
  uint64_t OpenBitSet::PopArray(const uint64_t* A, uint32_t wordOffset, uint32_t numWords)
  {
    int32_t n = wordOffset + numWords;
    int64_t tot = 0;
    int64_t tot8 = 0;
    int64_t ones = 0;
    int64_t twos = 0;
    int64_t fours = 0;

    int32_t i = wordOffset;
    for (; i <= n - 8; i += 8)
    {
      int64_t twosA;
      CSA(twosA, ones, ones, A[i], A[i + 1]);

      int64_t twosB;
      CSA(twosB, ones, ones, A[i + 2], A[i + 3]);

      int64_t foursA;
      CSA(foursA, twos, twos, twosA, twosB);

      CSA(twosA, ones, ones, A[i + 4], A[i + 5]);

      CSA(twosB, ones, ones, A[i + 6], A[i + 7]);

      int64_t foursB;
      CSA(foursB, twos, twos, twosA, twosB);

      int64_t eights;
      CSA(eights, fours, fours, foursA, foursB);

      tot8 += pop(eights);
    }

    // Handle trailing words in a binary-search manner.
    // Derived from the loop above by setting specific elements to 0.

    if (i <= n - 4)
    {
      int64_t twosA;
      CSA(twosA, ones, ones, A[i], A[i + 1]);

      int64_t twosB;
      CSA(twosB, ones, ones, A[i + 2], A[i + 3]);

      int64_t foursA;
      CSA(foursA, twos, twos, twosA, twosB);

      int64_t eights = fours & foursA;
      fours = fours ^ foursA;

      tot8 += pop(eights);
      i += 4;
    }

    if (i <= n - 2)
    {
      int64_t twosA;
      CSA(twosA, ones, ones, A[i], A[i + 1]);

      int64_t foursA = twos & twosA;
      twos = twos ^ twosA;

      int64_t eights = fours & foursA;
      fours = fours ^ foursA;

      tot8 += pop(eights);
      i += 2;
    }

    if (i < n)
      tot += pop(A[i]);
    tot += (pop(fours) << 2) + (pop(twos) << 1) + pop(ones) + (tot8 << 3);
    return tot;
  }
}//namespace monad
