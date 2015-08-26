// Copyright (c) 2015 Jun Tsai. All rights reserved.

#ifndef MONAD_SPARSE_BIT_SET_H_
#define MONAD_SPARSE_BIT_SET_H_

#include <stdint.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "bit_set.h"

namespace monad {
  class LeapFrogCallBack;
  class Uint64Array;
  class SparseBitSet :public BitSet<SparseBitSet>{
  public:
    SparseBitSet(uint32_t length);
    uint32_t GetLength(){return _length;}
    uint32_t Cardinality() const;
    void Set(uint32_t i);
    bool Get(uint32_t i);
    //序列化使用
    void ReadIndice(uint32_t index,uint64_t i){
      _indices[index]=i;
    }
    void CreateBit(uint32_t index,uint32_t size);
    void ReadBitBlock(uint32_t index,uint32_t block_index,uint64_t i);
    void ReadNonZero(uint32_t nonZero){
      _nonZeroLongCount = nonZero;
    }

    virtual BitSetIterator* ToIterator();
    uint32_t NextSetBit(uint32_t i) const;
    uint32_t PreSetBit(uint32_t i);
    void Clear(uint32_t i);
    void Clear(uint32_t from, uint32_t to);
    int32_t Weight(){
      return _weight;
    };
    void SetWeight(int32_t weight){
      _weight = weight;
    };
    SparseBitSet* Clone();

    virtual ~SparseBitSet();

    //common
    uint32_t GetWordsLength(){ return GetLength();};
    bool FastGet(uint32_t i){return Get(i);};
    void And(const SparseBitSet& other);
    void Or(const SparseBitSet& other);
    void Remove(const SparseBitSet& other);
    void operator+=(const SparseBitSet& other){ Or(other);}
    void operator&=(const SparseBitSet& other){ And(other);};
    void operator-=(const SparseBitSet& other){ Remove(other);};

    void Debug();
  private:
    bool consistent(uint32_t index);
    void insertBlock(uint32_t i4096, uint32_t i64, uint32_t i);
    void insertLong(uint32_t i4096, uint32_t i64, uint32_t i, uint64_t index);
    void Or(uint32_t i4096, uint64_t index, Uint64Array* bits, uint32_t nonZeroLongCount);
    void leapFrog(const SparseBitSet& other, LeapFrogCallBack& callback);
    void removeLong(uint32_t i4096, uint32_t i64, uint64_t index, uint32_t o);
    void And(uint32_t i4096, uint32_t i64, uint64_t mask);
    void clearWithinBlock(uint32_t i4096, uint32_t from, uint32_t to);
    uint32_t firstDoc(uint32_t i4096) const;
    uint64_t longBits(uint64_t index, Uint64Array* bits, uint32_t i64);
    uint32_t lastDoc(uint32_t i4096);
    uint32_t _blockCount;
    uint64_t* _indices;
    Uint64Array** _bits;
    uint32_t _length;
    uint32_t _nonZeroLongCount;
    int32_t _weight;
  };
}
#endif //MONAD_SPARSE_BIT_SET_H_
