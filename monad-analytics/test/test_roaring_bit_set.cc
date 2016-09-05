// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <roaring_bit_set_iterator.h>

#include "roaring_bit_set.h"
using namespace monad;
class RoaringBitSetTest: public ::testing::Test {
  protected:
  RoaringBitSetTest() {
    }
  virtual ~RoaringBitSetTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(RoaringBitSetTest, TestRead) {
  RoaringBitSet bit_set;
  bit_set.Set(1000);
  bit_set.Set(3000);
  bit_set.Set(5000);
  bit_set.Set(15000);
  bit_set.Set(40);
  /*
  bit_set.ReadNonZero(3);
  bit_set.ReadIndice(0,70368744210432);
  bit_set.ReadIndice(1,16384);

  bit_set.CreateBit(0, 3);
  bit_set.ReadBitBlock(0,0,1099511627776);
  bit_set.ReadBitBlock(0,1,72057594037927936);
  bit_set.ReadBitBlock(0,2,0);

  bit_set.CreateBit(1, 1);
  bit_set.ReadBitBlock(1,0,256);
  bit_set.Set(40);
  */



  ASSERT_TRUE(bit_set.Get(40));
  ASSERT_TRUE(bit_set.Get(1000));
  ASSERT_FALSE(bit_set.Get(2000));
  ASSERT_TRUE(bit_set.Get(3000));
  ASSERT_TRUE(bit_set.Get(5000));
  bit_set.Optimize();

  BitSetIterator* iter = bit_set.ToIterator();
  while(iter->NextDoc() != BitSetIterator::NO_MORE_DOCS){
    printf("%d \n",iter->DocId());
  }

}
/*
TEST_F(RoaringBitSetTest, TestBitCount) {
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
}
TEST_F(RoaringBitSetTest, TestGetSet) {

  SparseBitSet bit_set(5000);
  bit_set.Set(4099);
  bit_set.Set(188);
  bit_set.Set(288);
  //bit_set.set(4099);
  //uint32_t index = 10;
  ASSERT_TRUE(bit_set.Get(4099));
  ASSERT_TRUE(bit_set.Get(188));
  //index += 64;
  ASSERT_TRUE(bit_set.Get(288));
  ASSERT_FALSE(bit_set.Get(388));
}
TEST_F(RoaringBitSetTest, TestUnion) {
  SparseBitSet bit_set(10000);
  bit_set.Set(1000);
  bit_set.Set(3000);
  bit_set.Set(5000);
  bit_set.Set(7000);
  bit_set.Set(9000);


  SparseBitSet bit_set1(10000);
  bit_set1.Set(2000);
  bit_set1.Set(4000);
  bit_set1.Set(6000);
  bit_set1.Set(8000);
  bit_set1.Set(9000);

  bit_set += bit_set1;
  
  bit_set.Debug();

  ASSERT_TRUE(bit_set.Get(1000));
  ASSERT_FALSE(bit_set.Get(1500));
  ASSERT_EQ(1000,bit_set.PreSetBit(1500));
  ASSERT_TRUE(bit_set.Get(2000));
  ASSERT_TRUE(bit_set.Get(3000));
  ASSERT_TRUE(bit_set.Get(4000));
  ASSERT_TRUE(bit_set.Get(5000));
  ASSERT_TRUE(bit_set.Get(6000));
  ASSERT_TRUE(bit_set.Get(7000));
  ASSERT_TRUE(bit_set.Get(8000));
  ASSERT_TRUE(bit_set.Get(8000));
  ASSERT_EQ(9000,bit_set.NextSetBit(8001));
  ASSERT_TRUE(bit_set.Get(9000));
  ASSERT_EQ(9000,bit_set.PreSetBit(9500));
  ASSERT_FALSE(bit_set.Get(9900));
}

TEST_F(RoaringBitSetTest, TestAnd) {

  SparseBitSet bit_set(10000);
  bit_set.Set(1000);
  bit_set.Set(3000);
  bit_set.Set(5000);
  bit_set.Set(7000);
  bit_set.Set(9000);


  SparseBitSet bit_set1(10000);
  bit_set1.Set(2000);
  bit_set1.Set(3000);
  bit_set1.Set(4000);
  bit_set1.Set(6000);
  bit_set1.Set(7000);
  bit_set1.Set(8000);
  bit_set1.Set(9000);

  bit_set &= bit_set1;

  bit_set.Debug();

  ASSERT_FALSE(bit_set.Get(1000));
  ASSERT_TRUE(bit_set.Get(3000));
  ASSERT_TRUE(bit_set.Get(7000));
  ASSERT_FALSE(bit_set.Get(8000));
  ASSERT_FALSE(bit_set.Get(9900));
}
TEST_F(RoaringBitSetTest, TestNot) {

  SparseBitSet bit_set(10000);
  bit_set.Set(1000);
  bit_set.Set(1500);
  bit_set.Set(3000);
  bit_set.Set(3500);
  bit_set.Set(5000);
  bit_set.Set(7000);
  bit_set.Set(9000);


  SparseBitSet bit_set1(10000);
  bit_set1.Set(2000);
  bit_set1.Set(3000);
  bit_set1.Set(4000);
  bit_set1.Set(6000);
  bit_set1.Set(7000);
  bit_set1.Set(8000);
  bit_set1.Set(9000);

  bit_set -= bit_set1;
  
  bit_set.Debug();

  ASSERT_TRUE(bit_set.Get(1000));
  ASSERT_TRUE(bit_set.Get(3500));
  ASSERT_TRUE(bit_set.Get(1500));
  ASSERT_FALSE(bit_set.Get(3000));
  ASSERT_FALSE(bit_set.Get(7000));
  ASSERT_FALSE(bit_set.Get(9000));
}
*/

