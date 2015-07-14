// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_TOP_BIT_SET_ITERATOR_H_
#define MONAD_TOP_BIT_SET_ITERATOR_H_

#include "open_bit_set_iterator.h"
#include "top_bit_set.h"

namespace monad{
 class TopBitSetIterator{
 public:
   TopBitSetIterator(TopBitSet* bit_set):_bit_set(bit_set),_cur_doc(NULL){
     _i = 0;
   };
   uint32_t NextDoc(){
     if( _i< static_cast<int32_t>(_bit_set->DocCount())){
       _cur_doc = _bit_set->Get(_i++);
       return _cur_doc->doc;
     }
     _cur_doc = NULL;
     return OpenBitSetIterator::NO_MORE_DOCS;
   };
   uint32_t DocId(){
     return  _cur_doc == NULL ? OpenBitSetIterator::NO_MORE_DOCS : _cur_doc->doc;
   };
   TopDoc* Doc(){
     return _cur_doc;
   };
 private:
   TopBitSet* _bit_set;
   TopDoc* _cur_doc;
   int32_t _i;
 };//class TopBitSetIterator 
}//namespace monad
#endif //MONAD_TOP_BIT_SET_ITERATOR_H_
