// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_ROARING_BIT_SET_ITERATOR_H_
#define MONAD_ROARING_BIT_SET_ITERATOR_H_
#include "roaring_bit_set.h"

#include "bit_set.h"

namespace monad
{

  /**
   * SparseBitSet的迭代器
   * @param bit_set
   */
  class RoaringBitSetIterator:public BitSetIterator{
    public :
    RoaringBitSetIterator(RoaringBitSet* bit_set){
      _bit_set = bit_set;
      _cur_doc_id = -1;
      pos = 0;
      hs = 0;
      iter = NULL;

      nextContainer();
    }
    uint32_t NextDoc(){
      if(! iter) {
        _cur_doc_id = NO_MORE_DOCS;
        return _cur_doc_id;
      }

      _cur_doc_id = iter[_current_array_pos];
      _current_array_pos += 1;
      if(_current_array_pos == _current_array_length) {
        nextContainer();
      }
      return _cur_doc_id;
    }
    uint32_t DocId(){
      return _cur_doc_id;
    }
    virtual ~RoaringBitSetIterator(){
      clearIter();
    }
private:
  void clearIter(){
    if(iter){
      free(iter);
      iter = NULL;
    }
  }
  void nextContainer() {
    clearIter();
    if (pos < _bit_set->_underlying->high_low_container->size) {
      void* container = this->_bit_set->_underlying->high_low_container->containers[pos];
      uint8_t typecode = _bit_set->_underlying->high_low_container->typecodes[pos];
      hs = _bit_set->_underlying->high_low_container->keys[pos] << 16;
      _current_array_length = container_get_cardinality(container,typecode);
      iter = (uint32_t*)malloc(_current_array_length * sizeof(uint32_t));
      container_to_uint32_array(iter,container,typecode,hs);
      _current_array_pos = 0;
      pos += 1;
      //loop next container if current container is empty
      if(_current_array_length == 0)
        nextContainer();
    }
  }

    RoaringBitSet* _bit_set;
    uint32_t* iter;
    int32_t _cur_doc_id; //当前数据的ID号
    uint32_t _current_array_length;
    uint32_t _current_array_pos;
    uint32_t pos;
    uint32_t hs;

  }; //class RoaringBitSetIterator
} //namespace monad
#endif //MONAD_ROARING_BIT_SET_ITERATOR_H_
