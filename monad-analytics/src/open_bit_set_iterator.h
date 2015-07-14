// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_ITERATOR_H_
#define MONAD_OPEN_BIT_SET_ITERATOR_H_
#include "open_bit_set.h"

namespace monad
{

  /**
   * OpenBitSet的迭代器
   * @param bit_set
   */
  class OpenBitSetIterator{
    public :
    OpenBitSetIterator(OpenBitSet& bit_set)
    : _bits(bit_set.GetBits()), _num_words(bit_set.GetNumWords()) {
      _word = 0;
      _word_shift = 0;
      _index_array = 0;
      _cur_doc_id = -1;
      _i = -1;
    }
    uint32_t NextDoc();
    uint32_t DocId(){
      return _cur_doc_id;
    }
    static const uint32_t BIT_LIST[];
    static const uint32_t NO_MORE_DOCS;

private:
    void Shift();
    uint64_t* _bits; //包含的数据
    uint32_t _num_words;//拥有的uint64_t的个数
    uint64_t _word; //当前操作的数据
    int32_t _word_shift; //数据偏移值
    uint32_t _index_array;//索引数组
    int32_t _cur_doc_id; //当前数据的ID号
    int32_t _i; //全局变量

  }; //class OpenBitSetIterator
} //namespace monad
#endif //MONAD_OPEN_BIT_SET_ITERATOR_H_
