// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>

#include "open_bit_set.h"
using namespace monad;
class OpenBitSetTest: public ::testing::Test {
  protected:
  OpenBitSetTest() {
    }
  virtual ~OpenBitSetTest() {
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

TEST_F(OpenBitSetTest, TestOverflow) {
  OpenBitSet bit_set(1);
  bit_set.FastSet((uint32_t)46);
  bit_set.Set(123);
  ASSERT_TRUE(bit_set.Get((uint32_t)46));
  ASSERT_TRUE(bit_set.Get((uint32_t)123));
}
TEST_F(OpenBitSetTest, TestReadByte) {
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
TEST_F(OpenBitSetTest, TestGetSet) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);
  uint32_t index = 10;
  ASSERT_TRUE(bit_set.FastGet(index));
  index += 64;
  ASSERT_TRUE(bit_set.FastGet(index));
  ASSERT_FALSE(bit_set.FastGet(index+1));
  bit_set.EnsureCapacityWords(20);

  ASSERT_EQ(2,bit_set.BitCount());
}
TEST_F(OpenBitSetTest, TestRemove) {
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
TEST_F(OpenBitSetTest, TestUnion) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);


  OpenBitSet bit_set1(20);
  bit_set1.ReadLong(1<< 10,2);
  bit_set+= bit_set1;
  ASSERT_EQ(20,bit_set.GetNumWords());

  uint32_t index = 10;
  index += 64;
  index += 64;
  ASSERT_TRUE(bit_set1.FastGet(index));
  ASSERT_TRUE(bit_set.FastGet(index));

  ASSERT_EQ(3,bit_set.BitCount());
}

