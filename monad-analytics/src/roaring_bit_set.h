// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef ROARING_BIt_SET_H_
#define ROARING_BIt_SET_H_

#include <stdint.h>
#include "bit_set.h"

namespace monad{
  class Container{
  protected:
    virtual void Add(uint16_t item)=0;
  };
  class RoaringArray{

  };
  class RoaringBitSet:BitSet<RoaringBitSet>{
  public:
    void Set(uint32_t i);
    bool Get(uint32_t i);
  };
}
#endif //ROARING_BIt_SET_H_
