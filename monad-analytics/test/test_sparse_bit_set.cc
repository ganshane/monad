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

//采用big endian的模式来转换
static inline void ConvertInt64ToBytes(const int64_t &value,std::string &val){
  char buf[8];
  buf[0] = (value >> 56) & 0xff;
  buf[1] = (value >> 48) & 0xff;
  buf[2] = (value >> 40) & 0xff;
  buf[3] = (value >> 32) & 0xff;
  buf[4] = (value >> 24) & 0xff;
  buf[5] = (value >> 16) & 0xff;
  buf[6] = (value >> 8) & 0xff;
  buf[7] = value & 0xff;
  val.append(buf,8);
}

TEST_F(SparseBitSetTest, TestOverflow) {
  OpenBitSet bit_set(1);
  bit_set.FastSet((uint32_t)46);
  bit_set.Set(123);
  ASSERT_TRUE(bit_set.Get((uint32_t)46));
  ASSERT_TRUE(bit_set.Get((uint32_t)123));
}
TEST_F(SparseBitSetTest, TestReadByte) {
  OpenBitSet bit_set(10);
  std::string  data;
  ConvertInt64ToBytes(1024,data);
  ConvertInt64ToBytes(1024,data);
  int8_t* n = (int8_t*)data.data();
  bit_set.ReadLong(n,0,1);
  uint32_t index = 10;
  ASSERT_TRUE(bit_set.FastGet(index));
  index += 64;
  ASSERT_TRUE(bit_set.FastGet(index));
  ASSERT_FALSE(bit_set.FastGet(index+1));
  bit_set.EnsureCapacityWords(20);

  ASSERT_EQ(2,bit_set.BitCount());
}
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
TEST_F(SparseBitSetTest, TestRemove) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);

  OpenBitSet bit_set1(2);
  bit_set1.ReadLong(1<< 10,1);
  bit_set-= bit_set1;
  uint32_t index = 10;
  ASSERT_TRUE(bit_set.FastGet(index));
  index += 64;
  ASSERT_FALSE(bit_set.FastGet(index));
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

