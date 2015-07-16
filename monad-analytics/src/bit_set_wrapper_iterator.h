// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_WRAPPER_ITERATOR_H_
#define MONAD_OPEN_BIT_SET_WRAPPER_ITERATOR_H_
#include <vector>

namespace monad{

  template<typename T>
  struct BitSetRegion;
  /**
   * 针对wrapper中各个分区数据进行迭代的操作类
   * @param wrapper 待操作的wrapper对象
   */
  template<typename WRAPPER,typename BIT_SET>
  class BitSetWrapperIterator{
  public:
    BitSetWrapperIterator(WRAPPER* wrapper);
    /**
         * 下一个数据区域
         * @return 区域对象，如果到达结尾，则返回NULL
         */
    BitSetRegion<BIT_SET>* NextRegion();
    /**
     * 返回当前的数据区域
     * @return 数据区域对象
     */
    BitSetRegion<BIT_SET>* Region();
  private:
    //迭代器对象
    typename std::vector<BitSetRegion<BIT_SET>*>::iterator _it;
    WRAPPER* _wrapper;//操作的当前wrapper对象
    BitSetRegion<BIT_SET>* _region;//当前数据区域对象
  }; //class BitSetWrapper
  template<typename WRAPPER, typename BIT_SET>
  BitSetWrapperIterator<WRAPPER, BIT_SET>::BitSetWrapperIterator(WRAPPER* wrapper)
  : _wrapper(wrapper), _region(NULL) {
    _it = _wrapper->_data.begin();
  };

  template<typename WRAPPER, typename BIT_SET>
  BitSetRegion<BIT_SET>* BitSetWrapperIterator<WRAPPER, BIT_SET>::NextRegion() {
    if (_it != _wrapper->_data.end()) {
      _region = *_it;
      _it++;
    } else {
      _region = NULL;
    }
    return _region;
  };
  template<typename WRAPPER, typename BIT_SET>
  BitSetRegion<BIT_SET>* BitSetWrapperIterator<WRAPPER, BIT_SET>::Region() {
    return _region;
  };
} //namespace monad
#endif //MONAD_OPEN_BIT_SET_WRAPPER_ITERATOR_H_

