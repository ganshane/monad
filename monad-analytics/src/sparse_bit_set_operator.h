// Copyright (c) 2015 Jun Tsai. All rights reserved.

#ifndef MONAD_SPARSE_BIT_SET_OPERATOR_H_
#define MONAD_SPARSE_BIT_SET_OPERATOR_H_

#include "sparse_bit_set.h"
#include "bit_set_operator.h"

namespace monad{
  /**
   *
   * 针对单一的SparseBitSet进行各种操作
   */
  class SparseBitSetOperator :public BitSetOperator<SparseBitSet>{
  };
}
#endif //MONAD_OPEN_BIT_SET_OPERATOR_H_
