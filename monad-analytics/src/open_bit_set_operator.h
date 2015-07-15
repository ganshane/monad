// Copyright (c) 2009-2014 Jun Tsai. All rights reserved.

#ifndef MONAD_OPEN_BIT_SET_OPERATOR_H_
#define MONAD_OPEN_BIT_SET_OPERATOR_H_

#include "open_bit_set.h"
#include "top_bit_set.h"
#include "bit_set_operator.h"

namespace monad{
  /**
   *
   * 针对单一的OpenBitSet进行各种操作
   */
  class OpenBitSetOperator:public BitSetOperator<OpenBitSet>{
  };
}
#endif //MONAD_OPEN_BIT_SET_OPERATOR_H_
