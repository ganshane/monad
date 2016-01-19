// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>

#include "open_bit_set.h"
#include "open_bit_set_iterator.h"
using namespace monad;
class OpenBitSetIteratorTest: public ::testing::Test {
  protected:
  OpenBitSetIteratorTest() {
    }
  virtual ~OpenBitSetIteratorTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(OpenBitSetIteratorTest, TestGetSet) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);
  bit_set.ReadLong(1<< 10,9);
  uint32_t index = 10;
  ASSERT_TRUE(bit_set.FastGet(index));
  index += 64;
  ASSERT_TRUE(bit_set.FastGet(index));
  ASSERT_FALSE(bit_set.FastGet(index+1));

  OpenBitSetIterator it(bit_set);
  while(it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
}