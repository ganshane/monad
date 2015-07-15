// Copyright (c) 2015 Jun Tsai. All rights reserved.

#ifndef MONAD_BIT_SET_ITERATOR_H_
#define MONAD_BIT_SET_ITERATOR_H_
#include <stdint.h>
namespace monad{

  class BitSetIterator;
  template<typename T>
  class BitSet{
  public:
    //common
    uint32_t GetWordsLength();
    virtual BitSetIterator* ToIterator() = 0;
    virtual void And(const T& other) = 0;
    virtual void Or(const T& other) = 0;
    virtual void Remove(const T& other) = 0;
    virtual int32_t Weight() = 0;
    virtual BitSet<T>* Clone() = 0;
    void operator+=(const T& other){ Or(other);}
    void operator&=(const T& other){ And(other);};
    void operator-=(const T& other){ Remove(other);};
  };
  class BitSetIterator {
  public:
    virtual uint32_t NextDoc() = 0;
    virtual uint32_t DocId() = 0;
    static const uint32_t NO_MORE_DOCS;
    virtual ~BitSetIterator(){};
  };
}
#endif //MONAD_BIT_SET_ITERATOR_H_
