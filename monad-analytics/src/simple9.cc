// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.
#include "simple9.h"

#include <stdio.h>

namespace monad {
#define SELECTOR_MASK 0x0000000F
  //对数值表，用来快速查找
  /*
  static const int MultiplyDeBruijnBitPosition[32] =
  {
    0, 9, 1, 10, 13, 21, 2, 29, 11, 14, 16, 18, 22, 25, 3, 30,
    8, 12, 20, 28, 15, 17, 24, 7, 19, 27, 23, 6, 26, 5, 4, 31
  };
  //计算某一数的对数，实际上就是计算数字最高位的的位数
  static inline uint8_t Log2(uint32_t v){
    //先用最高位填充剩余的0,形成一个最高位数字的序列
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    return MultiplyDeBruijnBitPosition[(uint32_t)(v * 0x07C4ACDDU) >> 27];
  };
   */
  
  static const struct {
    uint32_t nitems;
    uint32_t nbits;
  } selectors[9] = {
    {28,  1},
    {14,  2},
    { 9,  3},
    { 7,  4},
    { 5,  5},
    { 4,  7},
    { 3,  9},
    { 2, 14},
    { 1, 28}
  };
  int Simple9::Compress(IntegerData& data,IntegerDataCompressed& result){
    uint32_t len = data.len;
    uint32_t index = 0,ret = 0,
      nitems=0,shift = 0,i=0;
    uint32_t *array = data.data;
    
    while (index < len) {
      for (uint32_t selector = 0; selector < 9; selector++) {
        ret = selector; //记录选择的编码格式
        shift = 4; //偏移量
        nitems = 0;//含有的数据个数
        
        for (i = index; i < len; i++) {
          if (nitems == selectors[selector].nitems)
            break;
          if (array[i] > (1UL << selectors[selector].nbits) - 1)
            break;
          
          ret |= (array[i] << shift);
          
          shift += selectors[selector].nbits;
          nitems++;
        }
        
        if (nitems == selectors[selector].nitems || index + nitems == len) {
          printf("selector:%u \n",selector);
          result.FastCopyData(&ret, sizeof(ret));
          index += nitems;
          break;
        }
      } /* End for selector ... */
    } /* End while index < n */
    return 1;
  };
  int Simple9::Uncompress(IntegerDataCompressed& result,IntegerData& origin_data){
    uint32_t data(0);
    uint32_t select;
    uint32_t *ptr;
    
    size_t nitems(0);
    uint32_t len = result.origin_len;
    
    ptr = origin_data.data;
    nitems = 0;
    
    while (nitems < len) {
      //读取数据
      memcpy(&data, result.result, sizeof(uint32_t));
      //获得模式编号
      select = data & SELECTOR_MASK;
      data >>= 4; //去掉模式数据，当前数据为真实数据
      
      switch (select) {
        case 0: /* 28 -- 1 bit elements */
          ptr[nitems++] = (data) & 1;
          ptr[nitems++] = (data >> 1) & 1;
          ptr[nitems++] = (data >> 2) & 1;
          ptr[nitems++] = (data >> 3) & 1;
          ptr[nitems++] = (data >> 4) & 1;
          ptr[nitems++] = (data >> 5) & 1;
          ptr[nitems++] = (data >> 6) & 1;
          ptr[nitems++] = (data >> 7) & 1;
          ptr[nitems++] = (data >> 8) & 1;
          ptr[nitems++] = (data >> 9) & 1;
          ptr[nitems++] = (data >> 10) & 1;
          ptr[nitems++] = (data >> 11) & 1;
          ptr[nitems++] = (data >> 12) & 1;
          ptr[nitems++] = (data >> 13) & 1;
          ptr[nitems++] = (data >> 14) & 1;
          ptr[nitems++] = (data >> 15) & 1;
          ptr[nitems++] = (data >> 16) & 1;
          ptr[nitems++] = (data >> 17) & 1;
          ptr[nitems++] = (data >> 18) & 1;
          ptr[nitems++] = (data >> 19) & 1;
          ptr[nitems++] = (data >> 20) & 1;
          ptr[nitems++] = (data >> 21) & 1;
          ptr[nitems++] = (data >> 22) & 1;
          ptr[nitems++] = (data >> 23) & 1;
          ptr[nitems++] = (data >> 24) & 1;
          ptr[nitems++] = (data >> 25) & 1;
          ptr[nitems++] = (data >> 26) & 1;
          ptr[nitems++] = (data >> 27) & 1;
          break;
          
        case 1: /* 14 -- 2 bit elements */
          ptr[nitems++] = (data) & 3;
          ptr[nitems++] = (data >> 2) & 3;
          ptr[nitems++] = (data >> 4) & 3;
          ptr[nitems++] = (data >> 6) & 3;
          ptr[nitems++] = (data >> 8) & 3;
          ptr[nitems++] = (data >> 10) & 3;
          ptr[nitems++] = (data >> 12) & 3;
          ptr[nitems++] = (data >> 14) & 3;
          ptr[nitems++] = (data >> 16) & 3;
          ptr[nitems++] = (data >> 18) & 3;
          ptr[nitems++] = (data >> 20) & 3;
          ptr[nitems++] = (data >> 22) & 3;
          ptr[nitems++] = (data >> 24) & 3;
          ptr[nitems++] = (data >> 26) & 3;
          break;
          
        case 2: /* 9 -- 3 bit elements (1 wasted bit) */
          ptr[nitems++] = (data) & 7;
          ptr[nitems++] = (data >> 3) & 7;
          ptr[nitems++] = (data >> 6) & 7;
          ptr[nitems++] = (data >> 9) & 7;
          ptr[nitems++] = (data >> 12) & 7;
          ptr[nitems++] = (data >> 15) & 7;
          ptr[nitems++] = (data >> 18) & 7;
          ptr[nitems++] = (data >> 21) & 7;
          ptr[nitems++] = (data >> 24) & 7;
          break;
          
        case 3: /* 7 -- 4 bit elements */
          ptr[nitems++] = (data) & 15;
          ptr[nitems++] = (data >> 4) & 15;
          ptr[nitems++] = (data >> 8) & 15;
          ptr[nitems++] = (data >> 12) & 15;
          ptr[nitems++] = (data >> 16) & 15;
          ptr[nitems++] = (data >> 20) & 15;
          ptr[nitems++] = (data >> 24) & 15;
          break;
          
        case 4: /* 5 -- 5 bit elements (3 wasted bits) */
          ptr[nitems++] = (data) & 31;
          ptr[nitems++] = (data >> 5) & 31;
          ptr[nitems++] = (data >> 10) & 31;
          ptr[nitems++] = (data >> 15) & 31;
          ptr[nitems++] = (data >> 20) & 31;
          break;
          
        case 5: /* 4 -- 7 bit elements */
          ptr[nitems++] = (data) & 127;
          ptr[nitems++] = (data >> 7) & 127;
          ptr[nitems++] = (data >> 14) & 127;
          ptr[nitems++] = (data >> 21) & 127;
          break;
          
        case 6: /* 3 -- 9 bit elements (1 wasted bit) */
          ptr[nitems++] = (data) & 511;
          ptr[nitems++] = (data >> 9) & 511;
          ptr[nitems++] = (data >> 18) & 511;
          break;
          
        case 7: /* 2 -- 14 bit elements */
          ptr[nitems++] = (data) & 16383;
          ptr[nitems++] = (data >> 14) & 16383;
          break;
          
        case 8: /* 1 -- 28 bit element */
          ptr[nitems++] = data;
          break;
      }
    }

    return 1;
  };
}//namespace monad
