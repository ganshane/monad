// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.
#include "variable_byte.h"
#include <string>

namespace monad {
  static inline void VariableByteEncode(uint32_t n,IntegerDataCompressed& result){
    while (n > 0x0000007f) {
      result.FastWriteByte((n & 0x0000007f) | 0x00000080);
      n >>= 7;
    }
    result.FastWriteByte(n & 0x0000007f);
  };
  static inline uint32_t VariableByteDecode(IntegerDataCompressed& result,uint32_t& byte_seq){
    uint32_t ret=0;
    uint32_t k=0;
    while (true) {
      uint8_t byte = result.result[byte_seq++];
      ret |= (byte & 0x0000007f) << (7 * k++);
      if(!(byte & 0x00000080))
        break;
    }
    return ret;
  }
  int VariableByteCompressor::Compress(IntegerData& integer_data,IntegerDataCompressed& result){
    for(uint32_t i=0;i<integer_data.len;i++){
      uint32_t n = *(integer_data.data + i);
      VariableByteEncode(n, result);
    }
    return 1;
  };
  int VariableByteCompressor::CompressWithDelta(IntegerData& integer_data,IntegerDataCompressed& result){
    uint32_t last(0);
    for(uint32_t i=0;i<integer_data.len;i++){
      uint32_t n = *(integer_data.data + i);
      VariableByteEncode(n-last, result);
      last = n;
    }
    return 1;
  };
  int VariableByteCompressor::UncompressWithDelta(IntegerDataCompressed& result,IntegerData& data){
    uint32_t last(0);
   for(uint32_t i=0,j=0;i<result.len;j++){
     last += VariableByteDecode(result, i);
     *(data.data +j) = last;
    }
    return 1;
  };
  int VariableByteCompressor::Uncompress(IntegerDataCompressed& result,IntegerData& data){
    for(uint32_t i=0,j=0;i<result.len;j++){
      *(data.data +j) = VariableByteDecode(result, i);
    }
    return 1;
  };
}