// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#include "open_bit_set_operator.h"
#include <assert.h>
#include <stdio.h>

#include "open_bit_set_iterator.h"
#include "priority_queue.h"
#include "top_bit_set.h"
#include "top_bit_set_iterator.h"

namespace monad{

  TopBitSet* OpenBitSetOperator::InPlaceAndTopWithPositionMerged(TopBitSet* coll[],int32_t size,int32_t min_freq){
    //采取多路合并策略
    TopBitSetIterator** its = new TopBitSetIterator*[size];
    uint32_t last_min = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t tmp_doc = OpenBitSetIterator::NO_MORE_DOCS;
    for(int i=0;i<size;i++){
      its[i] = new TopBitSetIterator(coll[i]);
      tmp_doc = its[i]->NextDoc();
      if(last_min > tmp_doc){
        last_min = tmp_doc;
      }
    }
    //说明没有数据
    if(last_min == OpenBitSetIterator::NO_MORE_DOCS){
      //清空内存
      for(int i=0;i<size;i++){
        delete its[i];
      }
      delete[] its;
      return new TopBitSet(0);
    }

    TopDoc* tmp_doc_obj(NULL);
    PriorityQueue<TopDoc> pq(1000,TopDocLessThanByFreq);
    TopDoc* doc(NULL);
    while(true){
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      if(doc)
        doc->Reset();
      else
        doc = new TopDoc((size >> 6) +1);
      for(int i=0;i<size;i++){
        tmp_doc = its[i]->DocId();
        tmp_doc_obj = its[i]->Doc();
        //printf("last min :%d next min:%d tmp doc:%d\n",last_min,next_min,tmp_doc);
        if(tmp_doc == last_min){
          doc->SetDoc(last_min);
          doc->MergePosition(tmp_doc_obj->position,tmp_doc_obj->position_len);
          /*
           * 频率的计算有两种模式：
           * 1）现有集合根据频率来计算
           * 2) 根据之前位置信息来计算一共的频率
           */
          doc->IncrementFreq(coll[i]->Weight());// * tmp_doc_obj->freq);
          tmp_doc = its[i]->NextDoc();
        }
        if(next_min > tmp_doc)
          next_min = tmp_doc;
      }

      //通过位置信息计算频率
      //doc->SetFreqByPosition();
      if(doc->freq >= static_cast<uint32_t>(min_freq)){
        doc= pq.InsertWithOverflow(doc); //插入到队列
      }
      if(next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_min = next_min;
    }
    if(doc)
      delete doc;
    //清空内存
    for(int i=0;i<size;i++){
      delete its[i];
    }
    delete[] its;

    //按照DOC进行排序
    uint32_t bit_set_size = pq.Size();
    PriorityQueue<TopDoc> doc_pq(bit_set_size,TopDocLessThanByDoc);
    for(uint32_t i=0;i<bit_set_size;i++){
      doc_pq.InsertWithOverflow(pq.Pop());
    }
    //构造结果
    TopBitSet* result = new TopBitSet(bit_set_size);
    for(uint32_t i=0;i<bit_set_size;i++){
      result->Add(doc_pq.Pop(),i);
    }

    return result;
  }
  TopBitSet* OpenBitSetOperator::InPlaceAndTop(OpenBitSet* coll[],int32_t size,int32_t min_freq){
    //采取多路合并策略
    OpenBitSetIterator** its = new OpenBitSetIterator*[size];
    uint32_t last_min = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = OpenBitSetIterator::NO_MORE_DOCS;
    uint32_t tmp_doc = OpenBitSetIterator::NO_MORE_DOCS;
    for(int i=0;i<size;i++){
      its[i] = new OpenBitSetIterator(*coll[i]);
      tmp_doc = its[i]->NextDoc();
      if(last_min > tmp_doc){
        last_min = tmp_doc;
      }
    }
    //说明没有数据
    if(last_min == OpenBitSetIterator::NO_MORE_DOCS){
      //清空内存
      for(int i=0;i<size;i++){
        delete its[i];
      }
      delete[] its;
      return new TopBitSet(0);
    }
    PriorityQueue<TopDoc> pq(1000,TopDocLessThanByFreq);
    TopDoc* doc = NULL;
    while(true){
      next_min = OpenBitSetIterator::NO_MORE_DOCS;
      if(doc)
        doc->Reset();//重复利用内存块
      else
        doc = new TopDoc((size >> 6) + 1);

      for(int i=0;i<size;i++){
        tmp_doc = its[i]->DocId();
        //printf("i:%d last min :%d next min:%d tmp doc:%d\n",i,last_min,next_min,tmp_doc);
        if(tmp_doc == last_min){
          doc->SetDoc(last_min);
          doc->SetPosition(i);//位置信息，按照bit位来讲应该是从1开始，而非0
          //通过给定的权重来记录频率，这样能够提升某一集合的重要度
          doc->IncrementFreq(coll[i]->Weight());
          tmp_doc = its[i]->NextDoc();
        }
        if(next_min > tmp_doc)
          next_min = tmp_doc;
      }

      if(doc->freq >= static_cast<uint32_t>(min_freq)){
        doc= pq.InsertWithOverflow(doc); //插入到队列
      }
      if(next_min == OpenBitSetIterator::NO_MORE_DOCS)
        break;
      last_min = next_min;
    }
    //删除最后一次使用的doc
    if(doc)
      delete doc;
    //清空内存
    for(int i=0;i<size;i++){
      delete its[i];
    }
    delete[] its;

    //按照DOC进行排序
    uint32_t bit_set_size = pq.Size();
    PriorityQueue<TopDoc> doc_pq(bit_set_size,TopDocLessThanByDoc);
    for(uint32_t i=0;i<bit_set_size;i++){
      doc_pq.InsertWithOverflow(pq.Pop());
    }
    //构造结果
    TopBitSet* result = new TopBitSet(bit_set_size);
    for(uint32_t i=0;i<bit_set_size;i++){
      result->Add(doc_pq.Pop(),i);
    }

    return result;
  }
  OpenBitSet* OpenBitSetOperator::InPlaceAnd(OpenBitSet* bit_coll[],int32_t size){
    int index = -1;
    uint32_t max_words = UINT32_MAX;
    uint32_t current_num = 0;
    for(int i=0;i<size;i++){
      current_num = bit_coll[i]->GetWordsLength();
      if(current_num < max_words){
        max_words = current_num;
        index = i;
      }
    }
    assert(index > -1);
    //printf("index:%d\n",index);

    OpenBitSet* result = bit_coll[index]->Clone();
    for(int i=0;i<size;i++){
      if(i != index)
        result->And(*bit_coll[i]);
    }
    return result;
  }
  OpenBitSet* OpenBitSetOperator::InPlaceOr(OpenBitSet* bit_coll[],int32_t size){
    int index = -1;
    uint32_t max_words = 0;
    uint32_t current_num = 0;
    for(int i=0;i<size;i++){
      current_num = bit_coll[i]->GetWordsLength();
      if(current_num > max_words){
        max_words = current_num;
        index = i;
      }
    }
    assert(index > -1);
    //printf("index:%d\n",index);

    OpenBitSet* result = bit_coll[index]->Clone();
    for(int i=0;i<size;i++){
      if(i != index)
        result->Or(*bit_coll[i]);
    }
    return result;
  }
  OpenBitSet* OpenBitSetOperator::InPlaceNot(OpenBitSet* bit_coll[],int32_t size){
    OpenBitSet* result = bit_coll[0]->Clone();
    for(int i=1;i<size;i++){
      result->Remove(*bit_coll[i]);
    }
    return result;
  }
}
