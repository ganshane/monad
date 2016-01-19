// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>

#include "variable_byte.h"
using namespace monad;
class VariableByteCompressorTest: public ::testing::Test {
  protected:
  VariableByteCompressorTest() {
    }
  virtual ~VariableByteCompressorTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};

TEST_F(VariableByteCompressorTest, TestCompressWithDelta) {
  VariableByteCompressor compressor;
  uint32_t data_len = 8;
  uint32_t data[]={10,35,100,170,370,29000,30000,30010};
  IntegerData input_data(data,data_len);
  IntegerDataCompressed out_data(data_len);
  compressor.CompressWithDelta(input_data,out_data);
  ASSERT_EQ(96, out_data.len * 8);
  IntegerData origin_data(out_data.origin_len);
  compressor.UncompressWithDelta(out_data,origin_data);
  ASSERT_EQ(data_len, origin_data.len);
  for(int i=0;i<data_len;i++){
    ASSERT_EQ(data[i],origin_data.data[i]);
  }
}
TEST_F(VariableByteCompressorTest, TestCompress2) {
  VariableByteCompressor compressor;
  uint32_t data_len = 8;
  uint32_t data[]={10,35,100,170,370,29000,30000,30010};
  IntegerData input_data(data,data_len);
  IntegerDataCompressed out_data(data_len);
  compressor.Compress(input_data,out_data);
  ASSERT_EQ(128, out_data.len * 8);
  IntegerData origin_data(out_data.origin_len);
  compressor.Uncompress(out_data,origin_data);
  ASSERT_EQ(data_len, origin_data.len);
  for(int i=0;i<data_len;i++){
    ASSERT_EQ(data[i],origin_data.data[i]);
  }
}

TEST_F(VariableByteCompressorTest, TestCompress) {
  VariableByteCompressor compressor;
  uint32_t data_len = 31;
  uint32_t data[data_len];
  for(int i=0;i<data_len;i++){
    data[i] = 1 << i;
  }
  IntegerData input_data(data,data_len);
  IntegerDataCompressed out_data(data_len);
  compressor.Compress(input_data,out_data);
  ASSERT_EQ(85, out_data.len);
  IntegerData origin_data(out_data.origin_len);
  compressor.Uncompress(out_data,origin_data);
  ASSERT_EQ(data_len, origin_data.len);
  for(int i=0;i<data_len;i++){
    ASSERT_EQ(1 << i,origin_data.data[i]);
  }
}

