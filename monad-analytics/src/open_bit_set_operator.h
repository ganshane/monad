// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_OPERATOR_H_
#define MONAD_OPEN_BIT_SET_OPERATOR_H_

#include "open_bit_set.h"
#include "top_bit_set.h"

namespace monad{
  /**
   *
   * 针对单一的OpenBitSet进行各种操作
   */
  class OpenBitSetOperator{
  public:
  /**
   * 针对集合进行And操作
   * @param coll 集合数据集合
   * @param size 集合的大小
   * @return And操作后的结果集
   */
    static OpenBitSet* InPlaceAnd(OpenBitSet* coll[],int32_t size);
    /**
     * 使用InPlaceAndTop的进行操作
     * @param coll 待操作的集合类
     * @param size 集合的长度
     * @param min_freq 最小频率
     * @return 进行InPlaceAndTop操作之后的集合对象
     */
    static TopBitSet* InPlaceAndTop(OpenBitSet* coll[],int32_t size,int32_t min_freq);
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
    static OpenBitSet* InPlaceOr(OpenBitSet* coll[],int32_t size);
    /**
     * 使用数组的第一个元素删除其余的集合
     * @param coll 集合数组
     * @param size 集合数组大小
     * @return not操作后的值
     */
    static OpenBitSet* InPlaceNot(OpenBitSet* coll[],int32_t size);
  };
}
#endif //MONAD_OPEN_BIT_SET_OPERATOR_H_
