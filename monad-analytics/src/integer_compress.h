// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_INTEGER_COMPRESS_H_
#define MONAD_INTEGER_COMPRESS_H_

#include <stdint.h>
#include <stdlib.h>
#include <string>

namespace monad {
  //压缩结果
  struct IntegerDataCompressed{
    uint8_t* result;//压缩结果
    uint32_t len;//实际压缩结果的长度
    uint32_t origin_len;//内存长度
  public:
    IntegerDataCompressed(uint32_t size){
      len = 0;
      result = static_cast<uint8_t*>(malloc(sizeof(uint32_t)*size+size));
      origin_len = size;
    }
    //快速写入不考虑是否超出内存长度
    void FastWriteByte(uint8_t i){
      //printf("write p:%u v:%u \n",len,i);
      result[len++] = i;
    };
    void FastCopyData(void* data,uint32_t size){
      memcpy(result+len,data,size);//TODO 支持BigEndian的机器
      len += size;
    }
    ~IntegerDataCompressed(){
      free(result);
    }
  };
  struct IntegerData{
    uint32_t* data;
    uint32_t len;
    
  public:
    IntegerData(uint32_t len){
      this->len = len;
      uint32_t size =sizeof(uint32_t) * len;
      this->data = static_cast<uint32_t*>(malloc(size));
      memset(data, 0, size);
      _self_memory_owner = true;
    }
    IntegerData(uint32_t* integer_data,uint32_t len){
      this->data = integer_data;
      this->len = len;
      _self_memory_owner = false;
    }
    ~IntegerData(){
      if(_self_memory_owner)
        free(data);
    }
  private:
    bool _self_memory_owner;
  };
} //namespace monad
#endif //MONAD_INTEGER_COMPRESS_H_