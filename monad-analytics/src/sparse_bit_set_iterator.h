// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_SPARSE_BIT_SET_ITERATOR_H_
#define MONAD_SPARSE_BIT_SET_ITERATOR_H_
#include "sparse_bit_set.h"
#include "bit_set.h"

namespace monad
{

  /**
   * SparseBitSet的迭代器
   * @param bit_set
   */
  class SparseBitSetIterator:public BitSetIterator{
    public :
    SparseBitSetIterator(SparseBitSet* bit_set){
      _bit_set = bit_set;
      _cur_doc_id = -1;
    }
    uint32_t NextDoc(){
      _cur_doc_id = _bit_set->NextSetBit(_cur_doc_id+1);
      return _cur_doc_id;
    }
    uint32_t DocId(){
      return _cur_doc_id;
    }
    virtual ~SparseBitSetIterator(){

    }
private:
    SparseBitSet* _bit_set;
    int32_t _cur_doc_id; //当前数据的ID号

  }; //class SparseBitSetIterator
} //namespace monad
#endif //MONAD_OPEN_BIT_SET_ITERATOR_H_
