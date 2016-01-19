// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>

#include "simple9.h"
using namespace monad;
class Simple9Test: public ::testing::Test {
  protected:
  Simple9Test() {
    }
  virtual ~Simple9Test() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};

TEST_F(Simple9Test, TestCompressWithDelta) {
  /*
  Simple9 compressor;
  uint32_t data_len = 8;
  uint32_t data[]={10,25,65,70,200,28630,1000,100};
  IntegerData input_data(data,data_len);
  IntegerDataCompressed out_data(data_len);
  compressor.Compress(input_data,out_data);
  ASSERT_EQ(160, out_data.len * 8);
  IntegerData origin_data(out_data.origin_len);

  compressor.Uncompress(out_data,origin_data);
  ASSERT_EQ(data_len, origin_data.len);
  for(int i=0;i<data_len;i++){
    ASSERT_EQ(data[i],origin_data.data[i]);
  }
   */
}
