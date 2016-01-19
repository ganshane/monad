// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_BIT_SET_WRAPPER_HOLDER_H_
#define MONAD_BIT_SET_WRAPPER_HOLDER_H_

#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <vector>

namespace monad {

  /**
   * 记录多个wrapper对象，
   * 主要在Java中使用，便于完全对象操作的方式
   * @deprecated 现在Java中使用数组进行传递
   */
  template<typename T>
  class BitSetWrapperHolder {
  public:
    BitSetWrapperHolder();
    virtual ~BitSetWrapperHolder();

    void AddWrapper(T* wrapper) {
      _data->push_back(wrapper);
    };

    int32_t Size() {
      return _data->size();
    };

    T* Get(uint32_t index) {
      assert(index < _data->size());
      return (*_data)[index];
    };
  private:
    std::vector<T*>* _data;
  };

  template<typename T>
  BitSetWrapperHolder<T>::BitSetWrapperHolder() {
    _data = new std::vector<T*>();
  };

  template<typename T>
  BitSetWrapperHolder<T>::~BitSetWrapperHolder() {
    if (_data)
      delete _data;
  };
} //namespae monad
#endif //MONAD_BIT_SET_WRAPPER_HOLDER_H_
