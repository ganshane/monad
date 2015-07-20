#include <stdio.h>
#include "sparse_bit_set_wrapper.h"

static uint32_t bitCount(uint64_t x)
{
  x = x - ((x >> 1ULL) & 0x5555555555555555ULL);
  x = (x & 0x3333333333333333ULL) + ((x >>2ULL) & 0x3333333333333333ULL);
  x = (x + (x >> 4ULL)) & 0x0f0f0f0f0f0f0f0fULL;
  x = x + (x >> 8ULL);
  x = x + (x >> 16ULL);
  x = x + (x >> 32ULL);
  return (uint32_t)x & 0x7f;
}
int main(){
  uint64_t m;
  m = static_cast<uint64_t> (1ULL << 64);
  printf("m::%lld \n",m);
  /*
  uint32_t length;
  monad::SparseBitSetWrapper wrapper;
  length = 100;
  wrapper.NewSeg(1,length);
  printf("c:%d \n",wrapper.BitCount());
  for(int i=0;i<length;i++){
    wrapper.FastSet(i);
    //printf("c:%d \n",wrapper.BitCount());
  }
  printf("c:%d \n",wrapper.BitCount());
  /*
  uint64_t num = 1 << 2;
  printf("count1 %d \n",bitCount(num));
  num |= 1LL << 4;
  printf("count1 %d \n",bitCount(num));
  num |= 1LL << 8;
  printf("count1 %d \n",bitCount(num));
  num |= 1LL << 10;
  printf("count1 %d \n",bitCount(num));
  num |= 1LL << 22;
  printf("count1 %d \n",bitCount(num));
  num |= 1LL << 32;
  printf("count1 %d \n",bitCount(num));
  num |= 1LL << 42;
  printf("count1 %d \n",bitCount(num));
   */
  return 0;
}
