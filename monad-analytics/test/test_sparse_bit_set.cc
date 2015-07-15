#include "gtest/gtest.h"
#include <vector>

#include "open_bit_set.h"
#include "sparse_bit_set.h"
using namespace monad;
class SparseBitSetTest: public ::testing::Test {
  protected:
  SparseBitSetTest() {
    }
  virtual ~SparseBitSetTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(SparseBitSetTest, TestGetSet) {
  SparseBitSet bit_set(5000);
  /*
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);
   */
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
TEST_F(SparseBitSetTest, TestUnion) {
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

TEST_F(SparseBitSetTest, TestAnd) {

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
TEST_F(SparseBitSetTest, TestNot) {

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

