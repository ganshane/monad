// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_TOP_BIT_SET_H_
#define MONAD_TOP_BIT_SET_H_

#include <assert.h>
#include <stdint.h>
#include <string.h>

#include "open_bit_set_iterator.h"

namespace monad {
  class TopBitSetIterator;
  struct TopDoc;
  /**
   * 分区Doc的对象
   */
  struct RegionDoc{
    uint32_t doc; //文档号
    uint32_t region;//分区
  };
  
  /**
   * 记录的TopDoc值
   */
  struct RegionTopDoc{
    TopDoc* top_doc; //TopDoc
    uint32_t region;//分区值
  };

  struct TopDoc {
    uint32_t doc;//Doc
    uint32_t freq;//频率
    uint64_t* position;//位置信息
    uint32_t  position_len;//位置信息的长度
  public:

    TopDoc(uint32_t size) {
      position = NULL;
      doc = 0;
      freq = 0;
      position_len = size;
      //生成内存空间
      uint32_t byte_len = size * sizeof (uint64_t);
      void* mem = malloc(byte_len);
      memset(mem, 0, byte_len);
      position = static_cast<uint64_t*> (mem);
    };
    ~TopDoc(){
      if(position)
        free(position);
    }
	void Reset(){
      //清空内存空间
      uint32_t byte_len = position_len * sizeof (uint64_t);
	  void* mem = static_cast<void*>(position);
      memset(mem, 0, byte_len);
	  //设置频率为0
	  freq=0;
	  doc=0;
	};

    void SetDoc(uint32_t doc) {
      this->doc = doc;
    };

    void SetPosition(uint32_t index) {
      uint32_t i = index >> 6; // div 64
      EnsureCapacityWords(i);
      uint32_t bit = (index & 0x3f); // mod 64
      uint64_t bitmask = 1ULL << bit;
      position[i] |= bitmask;
    };

    void MergePosition(uint64_t* p,uint32_t len) {
      EnsureCapacityWords(len);
      for(uint32_t i=0;i<len;i++)
        position[i] |= p[i];
    };

    void IncrementFreq(uint32_t freq) {
      this->freq += freq;
    };

    void SetFreqByPosition() {
      freq = 0;
      for(uint64_t i=0;i < position_len;i++){
        uint64_t x = position[i];
        x = x - ((x >> 1) & 0x5555555555555555ULL);
        x = (x & 0x3333333333333333ULL) + ((x >> 2) & 0x3333333333333333ULL);
        x = (x + (x >> 4)) & 0x0F0F0F0F0F0F0F0FULL;
        x = x + (x >> 8);
        x = x + (x >> 16);
        x = x + (x >> 32);
        freq += ((uint32_t) x) & 0x7F;
      };
    };
  private:
    void EnsureCapacityWords(uint32_t num_words) {
      if (position_len < num_words) {
        void* new_ptr = realloc(position, num_words * sizeof (uint64_t));
        assert(new_ptr);
        position = static_cast<uint64_t*> (new_ptr);
        for (uint32_t i = position_len ; i < num_words; i++) {
          position[i] = 0L;
        }
        position_len = num_words;
      }
    }
  };

  /**
   * 记录频次和位置信息的集合
   * @param size 集合的大小
   */
  class TopBitSet {
  public:

    TopBitSet(uint32_t size) {
      _docs = new TopDoc*[size]();
      _size = size;
      _weight = 1;
    }

    ~TopBitSet() {
      for (uint32_t i = 0; i < _size; i++)
        delete _docs[i];
      delete[] _docs;
    }
    uint32_t DocCount() {
      return _size;
    };
    TopDoc* Get(uint32_t index);
    void Add(TopDoc* doc, uint32_t index);
    TopBitSetIterator Iterator();
    uint32_t BitCount(){
      return _size;
    };

    void SetWeight(uint32_t weight) {
      this->_weight = weight;
    };

    uint32_t Weight() {
      return this->_weight;
    };
    static void FreeRegionTopDocArray(RegionTopDoc** docs,size_t len){
      for(uint32_t i=0;i<len;i++){
        delete docs[i];
      }
      delete[] docs;
    };
  private:
    TopDoc** _docs;
    uint32_t _size;
    uint32_t _weight;
  };
  bool TopDocLessThanByFreq(TopDoc* a,TopDoc* b);
  bool TopDocLessThanByDoc(TopDoc* a,TopDoc* b);
}
#endif //MONAD_TOP_BIT_SET_H_
