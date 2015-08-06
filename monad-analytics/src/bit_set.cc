#include "bit_set.h"

namespace monad{
  uint64_t BitSetUtils::UnsignedShift(uint64_t num, uint32_t shift) {
    return (shift & 0x3f) == 0 ? num : (((uint64_t)num >> 1) & 0x7fffffffffffffffLL) >> ((shift & 0x3f) - 1);
  }
  /*
  uint32_t BitSetUtils::UnsignedShift(uint32_t num, uint32_t shift) {
    return (shift & 0x1f) == 0 ? num : (((uint32_t)num >> 1) & 0x7fffffff) >> ((shift & 0x1f) - 1);
  }
  */
  uint32_t BitSetUtils::BitCount(uint64_t x) {
    x = x - (UnsignedShift(x, (int64_t)1) & 0x5555555555555555LL);
    x = (x & 0x3333333333333333LL) + (UnsignedShift(x, (int64_t)2) & 0x3333333333333333LL);
    x = (x + UnsignedShift(x, (int64_t)4)) & 0x0f0f0f0f0f0f0f0fLL;
    x = x + UnsignedShift(x, (int64_t)8);
    x = x + UnsignedShift(x, (int64_t)16);
    x = x + UnsignedShift(x, (int64_t)32);
    return (uint32_t) ((int32_t)x & 0x7f);
  }
}