// Copyright (c) 2015 Jun Tsai. All rights reserved.

#ifndef MONAD_SPARSE_BIT_SET_H_
#define MONAD_SPARSE_BIT_SET_H_

#include <stdint.h>
#include <stdio.h>

namespace monad {
  class Uint64Array{

  public:
    Uint64Array(uint32_t length){
      _length = length;
      _data = new uint64_t[length];
      memset(_data,0,sizeof(uint64_t)*length);
    }
    void Set(uint32_t index,uint64_t i){
      _data[index] = i;
    }
    virtual ~Uint64Array(){
      delete[] _data;
    }
    uint64_t operator[](const uint32_t index){
      assert(index<_length);
      return _data[index];
    }
    uint32_t _length;
    uint64_t* _data;
  };
  class SparseBitSet {
  public:
    SparseBitSet(uint32_t length);
    uint32_t Cardinality();
    void Set(uint32_t i);
    bool Get(uint32_t i);
    void ReadIndice(uint32_t index,uint64_t i){
      _indices[index]=i;
    }
    void CreateBit(uint32_t index,uint32_t size){
      _bits[index] = new Uint64Array(size);
    }
    void ReadBitBlock(uint32_t index,uint32_t block_index,uint64_t i){
      _bits[index]->Set(block_index,i);
    }
    void operator+=(const SparseBitSet& other);
    void operator&=(const SparseBitSet& other);
    uint32_t NextSetBit(uint32_t i);
    uint32_t PreSetBit(uint32_t i);

    virtual ~SparseBitSet();
    
    void Debug(){
      printf("indices:\n ===>");
      for(int i=0;i<_blockCount;i++){
        printf(" %llu",_indices[i]);
      }
      printf("===== end indices:\n");
    }
  private:
    bool consistent(uint32_t index);
    void insertBlock(uint32_t i4096, uint32_t i64, uint32_t i);
    void insertLong(uint32_t i4096, uint32_t i64, uint32_t i, uint64_t index);
    void Or(uint32_t i4096, uint64_t index, Uint64Array* bits, uint32_t nonZeroLongCount);
    uint32_t firstDoc(uint32_t i4096);
    uint64_t longBits(uint64_t index, Uint64Array* bits, uint32_t i64);
    uint32_t lastDoc(uint32_t i4096);
    uint32_t _blockCount;
    uint64_t* _indices;
    Uint64Array** _bits;
    uint32_t _length;
    uint32_t _nonZeroLongCount;
  };
}
#endif //MONAD_SPARSE_BIT_SET_H_
