// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <sdk/monad_sdk_impl.h>
#include <fstream>

using namespace monad;
const char path[100]= "test.db";
class MonadSDKTest: public ::testing::Test {
protected:
  MonadSDK* sdk;
  MonadSDKTest() {
    sdk = new MonadSDK(path);
  }
  virtual ~MonadSDKTest() {
    delete sdk;
    MonadSDK::Destroy(path);
  }
  virtual void SetUp() {
  }

  virtual void TearDown() {
  }
};
TEST_F(MonadSDKTest, TestRead) {
  std::string id("413028199009121514");
  sdk->PutId(id.c_str(),id.size());
  ASSERT_TRUE(sdk->ContainId(id.c_str(),id.size()));
  std::string id2("413028199009121524");
  ASSERT_FALSE(sdk->ContainId(id2.c_str(),id2.size()));
}
TEST_F(MonadSDKTest,TestKVPerformance){
  std::ifstream fin("/Users/jcai/Downloads/sfzh.txt", std::ios::in);

  char line[1024]={0};
  std::string id;
  auto i = 0;
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    sdk->PutKV(id,id);
    i++;
//    std::cout << i << " --> "<< id << std::endl;
    //if(i >100000)
    // break;
  }
  fin.clear();
  fin.close();
}
/*
TEST_F(MonadSDKTest,TestPerformance){
  std::ifstream fin("/Users/jcai/Downloads/sfzh.txt", std::ios::in);

  char line[1024]={0};
  std::string id;
  auto i = 0;
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    sdk->PutId(id.c_str(),id.size());
    i++;
//    std::cout << i << " --> "<< id << std::endl;
    //if(i >100000)
     // break;
  }
  fin.clear();
  fin.close();
}
 */
