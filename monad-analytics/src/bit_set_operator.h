// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_BIT_SET_OPERATOR_H_
#define MONAD_BIT_SET_OPERATOR_H_

#include "bit_set.h"
#include "priority_queue.h"
#include "roaring_bit_set.h"
#include "top_bit_set.h"
#include "top_bit_set_iterator.h"
#include "sparse_bit_set.h"

namespace monad{
  /**
   *
   * 针对单一的BitSet进行各种操作
   */
  template<typename T>
  class BitSetOperator{
  public:
  /**
   * 针对集合进行And操作
   * @param coll 集合数据集合
   * @param size 集合的大小
   * @return And操作后的结果集
   */
    static T* InPlaceAnd(T* coll[],int32_t size);
    /**
     * 使用InPlaceAndTop的进行操作
     * @param coll 待操作的集合类
     * @param size 集合的长度
     * @param min_freq 最小频率
     * @return 进行InPlaceAndTop操作之后的集合对象
     */
    static TopBitSet* InPlaceAndTop(T* coll[],int32_t size,int32_t min_freq);
    /**
     * 执行InPlaceAndTopWithPositionMerged算法操作
     * @param coll 待运算的集合对象
     * @param size 带运算的集合对象的长度
     * @param min_freq 最小频率
     * @return 操作之后的的集合对象
     */
    static TopBitSet* InPlaceAndTopWithPositionMerged(TopBitSet* coll[],int32_t size,int32_t min_freq);
    /**
     * 针对集合数组采用or计算
     * @param coll 集合数组
     * @param size 集合数组的大小
     * @return or操作之后的结果
     */
    static T* InPlaceOr(T* coll[],int32_t size);
    /**
     * 使用数组的第一个元素删除其余的集合
     * @param coll 集合数组
     * @param size 集合数组大小
     * @return not操作后的值
     */
    static T* InPlaceNot(T* coll[],int32_t size);
  };
  template<typename T>
  TopBitSet* BitSetOperator<T>::InPlaceAndTopWithPositionMerged(TopBitSet* coll[],int32_t size,int32_t min_freq){
    //采取多路合并策略
    TopBitSetIterator** its = new TopBitSetIterator*[size];
    uint32_t last_min = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    uint32_t tmp_doc = BitSetIterator::NO_MORE_DOCS;
    for(int i=0;i<size;i++){
      its[i] = new TopBitSetIterator(coll[i]);
      tmp_doc = its[i]->NextDoc();
      if(last_min > tmp_doc){
        last_min = tmp_doc;
      }
    }
    //说明没有数据
    if(last_min == BitSetIterator::NO_MORE_DOCS){
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
      next_min = BitSetIterator::NO_MORE_DOCS;
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
      if(next_min == BitSetIterator::NO_MORE_DOCS)
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
  template<typename T>
  TopBitSet* BitSetOperator<T>::InPlaceAndTop(T* coll[],int32_t size,int32_t min_freq){
    //采取多路合并策略
    BitSetIterator** its = new BitSetIterator*[size];
    uint32_t last_min = BitSetIterator::NO_MORE_DOCS;
    uint32_t next_min = BitSetIterator::NO_MORE_DOCS;
    uint32_t tmp_doc = BitSetIterator::NO_MORE_DOCS;
    for(int i=0;i<size;i++){
      //its[i] = new BitSetIterator(*coll[i]);
      its[i] = coll[i]->ToIterator();
      tmp_doc = its[i]->NextDoc();
      if(last_min > tmp_doc){
        last_min = tmp_doc;
      }
    }
    //说明没有数据
    if(last_min == BitSetIterator::NO_MORE_DOCS){
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
      next_min = BitSetIterator::NO_MORE_DOCS;
      if(doc)
        doc->Reset();//重复利用内存块
      else
        doc = new TopDoc((size >> 6) + 1);

      for(int i=0;i<size;i++){
        tmp_doc = its[i]->DocId();
//        printf("i:%d last min :%d next min:%d tmp doc:%d\n",i,last_min,next_min,tmp_doc);
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
      if(next_min == BitSetIterator::NO_MORE_DOCS)
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
  template<typename T>
  T* BitSetOperator<T>::InPlaceAnd(T* bit_coll[],int32_t size){
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

    T* result = bit_coll[index]->Clone();
    for(int i=0;i<size;i++){
      if(i != index)
        result->And(*bit_coll[i]);
    }
    return result;
  }
  template<typename T>
  T* BitSetOperator<T>::InPlaceOr(T* bit_coll[],int32_t size){
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

    T* result = bit_coll[index]->Clone();
    for(int i=0;i<size;i++){
      if(i != index)
        result->Or(*bit_coll[i]);
    }
    return result;
  }
  template<typename T>
  T* BitSetOperator<T>::InPlaceNot(T* bit_coll[],int32_t size){
    T* result = bit_coll[0]->Clone();
    for(int i=1;i<size;i++){
      result->Remove(*bit_coll[i]);
    }
    return result;
  }
  /**
   *
   * 针对单一的OpenBitSet进行各种操作
   */
  class OpenBitSetOperator:public BitSetOperator<OpenBitSet>{
  };
  /**
   *
   * 针对单一的SparseBitSet进行各种操作
   */
  class SparseBitSetOperator :public BitSetOperator<SparseBitSet>{
  };
  /**
   *
   * 针对单一的SparseBitSet进行各种操作
   */
  class RoaringBitSetOperator :public BitSetOperator<RoaringBitSet>{
  };
}
#endif //MONAD_OPEN_BIT_SET_OPERATOR_H_
