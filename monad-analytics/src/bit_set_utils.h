// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_BIT_SET_UTILS_H
#define MONAD_BIT_SET_UTILS_H
namespace monad{
  class BitSetUtils{
  public:
    inline static uint64_t RightShift(uint64_t i,uint32_t shift){
      return  i >> (shift & 63);
    }
    inline static uint64_t LeftShift(uint64_t i,uint32_t shift){
      return  i << (shift & 63);
    }
    inline static uint32_t NumberOfTrailingZeros(uint64_t i) {
      // HD, Figure 5-14
      uint32_t x, y;
      if (i == 0) return 64;
      int n = 63;
      y = (uint32_t)i; if (y != 0) { n = n -32; x = y; } else x = (uint32_t)(i>>32);
      y = x <<16; if (y != 0) { n = n -16; x = y; }
      y = x << 8; if (y != 0) { n = n - 8; x = y; }
      y = x << 4; if (y != 0) { n = n - 4; x = y; }
      y = x << 2; if (y != 0) { n = n - 2; x = y; }
      return n - ((x << 1) >> 31);
    }
    inline static uint32_t NumberOfLeadingZeros(uint64_t i) {
      // HD, Figure 5-6
      if (i == 0)
        return 64;
      uint32_t n = 1;
      uint32_t x = (uint32_t)(i >> 32);
      if (x == 0) { n += 32; x = (uint32_t)i; }
      if (x >> 16 == 0) { n += 16; x <<= 16; }
      if (x >> 24 == 0) { n +=  8; x <<=  8; }
      if (x >> 28 == 0) { n +=  4; x <<=  4; }
      if (x >> 30 == 0) { n +=  2; x <<=  2; }
      n -= x >> 31;
      return n;
    }
    inline static uint32_t BitCount(uint64_t x)
    {
      x = x - ((x >> 1ULL) & 0x5555555555555555ULL);
      x = (x & 0x3333333333333333ULL) + ((x >>2ULL) & 0x3333333333333333ULL);
      x = (x + (x >> 4ULL)) & 0x0f0f0f0f0f0f0f0fULL;
      x = x + (x >> 8ULL);
      x = x + (x >> 16ULL);
      x = x + (x >> 32ULL);
      return (uint32_t)x & 0x7f;
    }
  };
}
#endif //MONAD_BIT_SET_UTILS_H
