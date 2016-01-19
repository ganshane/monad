// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_VARIABLE_BYTE_H_
#define MONAD_VARIABLE_BYTE_H_

#include <string>

#include "integer_compress.h"

namespace monad {
  //
  /*
   * 每个byte的第一位为flag，表示是否继续使用下一个byte，剩下7位为有效位，
   * 所有的有效位组成数字的2进制表示。
   */
  class VariableByteCompressor{
  public:
    int Compress(IntegerData& data,IntegerDataCompressed& result);
    int Uncompress(IntegerDataCompressed& result,IntegerData& data);
    int CompressWithDelta(IntegerData& data,IntegerDataCompressed& result);
    int UncompressWithDelta(IntegerDataCompressed& result,IntegerData& data);
  };//class VariableByteCompressor
}//namespace monad
#endif //MONAD_VARIABLE_BYTE_H_