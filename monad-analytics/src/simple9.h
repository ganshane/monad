// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_SIMPLE9_H_
#define MONAD_SIMPLE9_H_
#include "integer_compress.h"
namespace monad{
  class Simple9{
  public:
    int Compress(IntegerData& data,IntegerDataCompressed& result);
    int Uncompress(IntegerDataCompressed& result,IntegerData& data);
  };//class Simple9
}//namespace monad
#endif //MONAD_SIMPLE9_H_