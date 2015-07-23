#include <stdio.h>

#include "emscripten.h"

//using namespace emscripten;


int main(){
  EM_ASM(
             if (analytics_onready){
               analytics_onready();
             }
        );
  //char url[]="http://127.0.0.1:9081";
  //emscripten_async_wget2_data(url,"POST","",NULL,true,&onload,NULL,NULL);
 // val xhr = val::global("XMLHttpRequest").new_();
  //xhr.call<val>("open", std::string("GET"), std::string("http://www.baidu.com"));
  /*
  uint64_t m;
  m = static_cast<uint64_t> (1ULL << 64);
  printf("m::%lld \n",m);
  */
  /*
  uint32_t length;
  monad::SparseBitSetWrapper wrapper;
  length = 100000000;
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
  printf("OK!!!\n");
  return 0;
}
